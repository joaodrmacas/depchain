package pt.tecnico.ulisboa.consensus;

import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.Node;
import pt.tecnico.ulisboa.consensus.message.AcceptMessage;
import pt.tecnico.ulisboa.consensus.message.CollectedMessage;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.NewEpochMessage;
import pt.tecnico.ulisboa.consensus.message.ReadMessage;
import pt.tecnico.ulisboa.consensus.message.StateMessage;
import pt.tecnico.ulisboa.consensus.message.WriteMessage;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.RequiresEquals;

public class EpochConsensus<T extends RequiresEquals> {
    private Node<T> member;
    private int epochNumber;
    private AtomicInteger keepEpochNumber;
    private AtomicBoolean readPhaseDone;
    private ConsensusState<T> state;
    private CollectedStates<T> collected = new CollectedStates<>(Config.NUM_MEMBERS);
    private EventCounter<T> writeCounts = new EventCounter<>();
    private EventCounter<T> acceptCounts = new EventCounter<>();
    private EventCounter<T> abortCounts = new EventCounter<>();
    private boolean hasBroadcastedNewEpoch = false;
    private CompletionService<Void> service;

    private class WritesOrAccepts {
        private T value;
        private boolean writes;

        public WritesOrAccepts(T value, boolean writes) {
            this.value = value;
            this.writes = writes;
        }

        public T getValue() {
            return value;
        }

        public boolean hasQuorumOfWrites() {
            return writes;
        }
    }

    public EpochConsensus(Node<T> member, AtomicInteger epochNumber, T valueToBeProposed, AtomicBoolean readPhaseDone) {
        this.member = member;
        this.epochNumber = epochNumber.get();
        this.keepEpochNumber = epochNumber;
        this.readPhaseDone = readPhaseDone;
        this.service = new ExecutorCompletionService<>(member.getExecutor());

        this.state = new ConsensusState<>(new WriteTuple<T>(valueToBeProposed, this.epochNumber));

        Logger.LOG("Creating epoch consensus");
    }

    public T start() {
        T valueDecided = null;

        while (true) {
            try {
                epoch();
            } catch (AbortedSignal abs) {
                Logger.LOG("Aborted: " + abs.getMessage());

                while (true) {
                    try {
                        receiveFromAll(ConsensusMessage.MessageType.NEWEPOCH);
                    } catch (AbortedSignal abs2) {
                        Logger.LOG("Aborted: " + abs2.getMessage());
                    }

                    if (shouldChangeEpoch())
                        break;
                }

                this.epochNumber++;

                readPhaseDone.set(false);

                resetCounters();

                continue;
            }
            break;
        }

        this.keepEpochNumber.set(this.epochNumber);

        return valueDecided;
    }

    public T epoch() throws AbortedSignal {
        Logger.LOG("Starting epoch");

        WritesOrAccepts writesOrAccepts = null;

        T valueToBeProposed = this.state.getMostRecentQuorumWritten().getValue();

        while (true) {
            // Leader
            if (getLeader(this.epochNumber) == member.getId()) {
                // aborts this epoch if is leader and has no value to propose
                if (valueToBeProposed == null) {
                    broadcastAbort();
                }

                if (!this.readPhaseDone.get()) {
                    sendToAll(new ReadMessage<>(this.epochNumber));

                    Logger.LOG("send READs done");

                    writesOrAccepts = receiveFromAll(ConsensusMessage.MessageType.STATE);
                    if (writesOrAccepts != null){
                        break;
                    }

                    Logger.LOG("receive STATEs done");

                    this.readPhaseDone.set(true);
                }

                this.state.sign(member.getPrivateKey());
                this.collected.addState(member.getId(), this.state);

                sendToAll(new CollectedMessage<>(this.collected, this.epochNumber));

                Logger.LOG("send COLLECTEDs done");
            } else { // Not leader
                writesOrAccepts = receiveFrom(ConsensusMessage.MessageType.READ, getLeader(epochNumber));
                if (writesOrAccepts != null)
                    break;

                Logger.LOG("receive READ done");

                this.state.sign(member.getPrivateKey());
                sendTo(new StateMessage<>(this.state, this.epochNumber), getLeader(epochNumber));

                Logger.LOG("send STATE done");

                writesOrAccepts = receiveFrom(ConsensusMessage.MessageType.COLLECTED, getLeader(epochNumber));
                if (writesOrAccepts != null)
                    break;

                Logger.LOG("receive COLLECTED done");

                if (!this.collected.verifyStates(member.getPublicKeys())) {
                    broadcastAbort();
                }
            }

            if (Logger.IS_DEBUGGING()) {
                Logger.DEBUG("Collected states:");
                System.err.println(this.collected + "\n");
            }

            T valueToWrite = decideFromCollected();
            sendToAll(new WriteMessage<>(valueToWrite, this.epochNumber));

            Logger.LOG("send WRITEs done");

            writeCounts.inc(valueToWrite, member.getId());
            writesOrAccepts = receiveFromAll(ConsensusMessage.MessageType.WRITE);

            Logger.LOG("receive WRITEs done");

            // if no value to write or accept we can proceed so we abort to a new epoch
            if (writesOrAccepts == null) {
                Logger.LOG("No value to write");

                if (Logger.IS_DEBUGGING()) {
                    Logger.DEBUG("Write counts: " + writeCounts);
                    Logger.DEBUG("Accept counts: " + acceptCounts);
                }

                broadcastAbort();
                throwAbort();
            }

            break; // end the one iteration loop
        }

        T valueDecided = writesOrAccepts.getValue();
        if (writesOrAccepts.hasQuorumOfWrites()) {
            acceptCounts.inc(valueDecided, member.getId());
        }

        sendToAll(new AcceptMessage<>(valueDecided, this.epochNumber));

        Logger.LOG("send ACCEPTs done");

        writesOrAccepts = receiveFromAll(ConsensusMessage.MessageType.ACCEPT);

        Logger.LOG("receive ACCEPTs done");

        // if no value to accept we can proceed so we abort to a new epoch
        if (writesOrAccepts == null) {
            Logger.LOG("No value to accept");

            broadcastAbort();
            throwAbort();
        }

        return writesOrAccepts.getValue();
    }

    public T decideFromCollected() throws AbortedSignal {
        Logger.LOG("Deciding from collected");

        if (this.collected.getStates().get(getLeader(epochNumber)) == null) {
            broadcastAbort();
        }

        WriteTuple<T> bestWT = null;

        int countCorrectStates = 0;

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            ConsensusState<T> state = this.collected.getStates().get(i);
            if (state != null) {
                countCorrectStates++;

                WriteTuple<T> wt = state.getMostRecentQuorumWritten();
                if (bestWT == null
                        || wt.getTimestamp() < bestWT.getTimestamp()) {
                    bestWT = wt;
                }
            }
        }

        if (countCorrectStates < Config.ALLOWED_FAILURES + 1) {
            broadcastAbort();
        }

        if (bestWT != null) {
            int countIncludedInWritesets = 0;
            for (int i = 0; i < Config.NUM_MEMBERS; i++) {
                ConsensusState<T> state = this.collected.getStates().get(i);
                if (state != null) {
                    Map<T, WriteTuple<T>> ws = state.getWriteSet();
                    for (T value : ws.keySet()) {
                        if (bestWT.getValue().equals(value)
                                && bestWT.getTimestamp() <= ws.get(value).getTimestamp()) {
                            countIncludedInWritesets++;
                            if (countIncludedInWritesets >= Config.ALLOWED_FAILURES + 1) {
                                return bestWT.getValue();
                            }
                        }
                    }
                }
            }
        }

        // we assume the leader write tuple if no best tupple is found
        // (an already decided tuple)
        return this.collected.getStates().get(getLeader(epochNumber)).getMostRecentQuorumWritten().getValue();
    }

    public int getLeader(int epocNumber) {
        return epocNumber % Config.NUM_MEMBERS;
    }

    public void throwAbort() throws AbortedSignal {
        Logger.LOG("Aborting");

        throw new AbortedSignal("Aborted");
    }

    public void broadcastAbort() {
        Logger.LOG("Broadcasting abort");

        abortCounts.inc(member.getId());

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                ConsensusMessage<T> message = new NewEpochMessage<>(this.epochNumber);
                member.sendToMember(i, message);
            }
        }

        hasBroadcastedNewEpoch = true;
    }

    public void resetCounters() {
        writeCounts = new EventCounter<>();
        acceptCounts = new EventCounter<>();
        abortCounts = new EventCounter<>();
    }

    public WritesOrAccepts checkForEvents() throws AbortedSignal {
        T value = acceptCounts.getExeeded(2 * Config.ALLOWED_FAILURES);
        if (value != null) {
            Logger.LOG("Quorum of accepts reached: " + value);

            return new WritesOrAccepts(value, false);
        }

        value = writeCounts.getExeeded(2 * Config.ALLOWED_FAILURES);
        if (value != null) {
            Logger.LOG("Quorum of writes reached: " + value);

            synchronized (this.state) {
                this.state.setMostRecentQuorumWritten(
                        new WriteTuple<T>(value, this.epochNumber));
            }

            return new WritesOrAccepts(value, true);
        }

        checkForAborts();

        return null;
    }

    public void checkForAborts() throws AbortedSignal {
        if (shouldBroadcastAborts()) {
            broadcastAbort();
        }
        if (shouldChangeEpoch()) {
            Logger.DEBUG("Conditiions met to change epoch");
            throwAbort();
        }
    }

    public boolean shouldBroadcastAborts() {
        return !hasBroadcastedNewEpoch && abortCounts.exceeded(Config.ALLOWED_FAILURES);
    }

    public boolean shouldChangeEpoch() {
        return abortCounts.exceeded(2*Config.ALLOWED_FAILURES);
    }

    public void sendToAll(ConsensusMessage<T> message) {
        Logger.LOG("Sending to all: " + message.getType());

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                member.sendToMember(i, message);
            }
        }

    }

    public void sendTo(ConsensusMessage<T> message, int receiverId) {
        Logger.LOG("Sending to " + receiverId + ": " + message.getType());

        member.sendToMember(receiverId, message);
    }

    public WritesOrAccepts receiveFromAll(ConsensusMessage.MessageType type) throws AbortedSignal {
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i == member.getId())
                continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            final int _i = i;
            this.service.submit(() -> receiveLoop(_i, type));

            Logger.DEBUG("Task " + i + " started");
        }

        Logger.LOG("Waiting for all tasks to finish");

        // Wait for all threads to finish
        for (int i = 0; i < Config.NUM_MEMBERS - 1; i++) {
            try {
                this.service.take();

                Logger.DEBUG((i+1) + " tasks finished");
            } catch (InterruptedException e) {
                Logger.LOG("Thread interrupted: " + e);
            }

            WritesOrAccepts writesOrAccepts = checkForEvents();
            if (writesOrAccepts != null) {
                Logger.DEBUG("Quorum reached");
                return writesOrAccepts;
            }
        }
        
        Logger.LOG("All tasks finished");
        return null;
    }

    public WritesOrAccepts receiveFrom(ConsensusMessage.MessageType type, int senderId) throws AbortedSignal {
        Future<Void> senderTask = null;
        
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i == member.getId()) continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            final int _i = i;
            Future<Void> future = this.service.submit(() -> receiveLoop(_i, type));
            if (i == senderId) senderTask = future;
        }

        try {
            // Wait for sender thread to finish
            senderTask.get();
        } catch (ExecutionException | InterruptedException e) {
            Logger.ERROR("Error executing the task: " + e, e);
        }

        return checkForEvents();
    }

    public Void receiveLoop(int senderId, ConsensusMessage.MessageType type) {
        int leaderId = getLeader(epochNumber);

        // Prevents byzantine process to send useless messages to keep the leader stuck
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < 2 * Config.CONSENSUS_LINK_TIMEOUT) {
            //Logger.DEBUG("Recv loop: time left: " + (2 * Config.LINK_TIMEOUT - (System.currentTimeMillis() - startTime)));
            ConsensusMessage<T> msg = member.peekConsensusMessageOrWait(senderId, Config.CONSENSUS_LINK_TIMEOUT);

            if (msg == null) {
                Logger.LOG(senderId + ") Timed out");
                return null;
            }

            // message from the future
            if (msg.getEpochNumber() > this.epochNumber) {
                Logger.LOG(senderId + ") Received message from future epoch");
                return null;
            }

            member.removeFirstConsensusMessage(senderId);

            // message from the past
            if (msg.getEpochNumber() < this.epochNumber) {
                Logger.LOG(senderId + ") Received message from past epoch");
                continue;
            }

            switch (msg.getType()) {
                case READ:
                    Logger.DEBUG(senderId + ") Received READ message");
                    // if not from leader, return
                    if (senderId != leaderId) {
                        Logger.LOG(senderId + ") READ msg not from leader");
                        return null;
                    }

                    // if received READ when not expected, continue
                    if (type != ConsensusMessage.MessageType.READ) {
                        Logger.LOG(senderId + ") " + type + " expected, got READ instead");
                        continue;
                    }

                    return null;

                case STATE:
                    Logger.DEBUG(senderId + ") Received STATE message");
                    // if received STATE when not expected, continue
                    if (type != ConsensusMessage.MessageType.STATE) {
                        Logger.LOG(senderId + ") " + type + " expected, got STATE instead");
                        continue;
                    }

                    StateMessage<T> stateMsg = (StateMessage<T>) msg;

                    // gotta check if each state received is well signed,
                    // if not dont store it (store null instead)
                    if (stateMsg.getState().verifySignature(member.getPublicKeys().get(senderId))) {
                        synchronized (this.collected) {
                            this.collected.addState(senderId, stateMsg.getState());
                        }
                    }

                    return null;

                case COLLECTED:
                    Logger.DEBUG(senderId + ") Received COLLECTED message");
                    // if not from leader, return
                    if (senderId != leaderId) {
                        Logger.LOG(senderId + ") COLLECTED msg not from leader");
                        return null;
                    }

                    // if received COLLECTED when not expected, continue
                    if (type != ConsensusMessage.MessageType.COLLECTED) {
                        Logger.LOG(senderId + ")" + type + " expected, got COLLECTED instead");
                        continue;
                    }
                    
                    CollectedMessage<T> collectedMsg = (CollectedMessage<T>) msg;
                    synchronized (this.collected) {
                        this.collected.overwriteWith(collectedMsg.getStates());
                    }

                    if (this.collected != null) {
                        Logger.LOG(senderId + ") Collected states:");
                        System.err.println(this.collected);
                    } else {
                        Logger.LOG(senderId + ") Collected states null");
                    }

                    return null;

                case WRITE:
                    Logger.DEBUG(senderId + ") Received WRITE message");
                    WriteMessage<T> writeMsg = (WriteMessage<T>) msg;

                    writeCounts.inc(writeMsg.getValue(), senderId);

                    synchronized (this.state) {
                        this.state.addToWriteSet(
                                new WriteTuple<T>(writeMsg.getValue(), this.epochNumber));
                    }

                    // if received WRITE when not expected, ignore but continue
                    // because we may have to receive accept message
                    if (type != ConsensusMessage.MessageType.WRITE) {
                        Logger.LOG(senderId + ") " + type + " expected, got WRITE instead");
                        continue;
                    }

                    return null;

                case ACCEPT:
                    Logger.DEBUG(senderId + ") Received ACCEPT message");
                    AcceptMessage<T> acceptMsg = (AcceptMessage<T>) msg;
                    acceptCounts.inc(acceptMsg.getValue(), senderId);

                    if (type != ConsensusMessage.MessageType.ACCEPT) {
                        Logger.LOG(senderId + ") " + type + " expected, got ACCEPT instead");
                    }

                    return null;

                case NEWEPOCH:
                    Logger.LOG(senderId + ") Received new epoch message");

                    abortCounts.inc(senderId);
                    return null;

                default:
                    Logger.LOG("Unknown message type: " + msg.getType());
                    continue;
            }
        }

        return null;
    }
}
