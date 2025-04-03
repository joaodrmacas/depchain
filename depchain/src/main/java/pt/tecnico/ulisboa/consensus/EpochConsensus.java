package pt.tecnico.ulisboa.consensus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.consensus.message.AcceptMessage;
import pt.tecnico.ulisboa.consensus.message.CollectedMessage;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.NewEpochMessage;
import pt.tecnico.ulisboa.consensus.message.ReadMessage;
import pt.tecnico.ulisboa.consensus.message.StateMessage;
import pt.tecnico.ulisboa.consensus.message.WriteMessage;
import pt.tecnico.ulisboa.server.Server;
import pt.tecnico.ulisboa.utils.types.Logger;
public class EpochConsensus {
    private Server member;
    private AtomicInteger epochNumber;
    private int consensusIndex;
    private AtomicBoolean readPhaseDone;
    private ConsensusState state;
    private CollectedStates collected = new CollectedStates<>(Config.NUM_MEMBERS);
    private EventCounter<T> writeCounts = new EventCounter<>();
    private EventCounter<T> acceptCounts = new EventCounter<>();
    private EventCounter<T> abortCounts = new EventCounter<>();
    private boolean hasBroadcastedNewEpoch = false;
    private CompletionService<Boolean> service;
    private WritesOrAccepts woa;

    private class WritesOrAccepts {
        private T value;
        private boolean toAccept;

        public WritesOrAccepts(T value, boolean toAccept) {
            this.value = value;
            this.toAccept = toAccept;
        }

        public T getValue() {
            return value;
        }

        public boolean isToAccept() {
            return toAccept;
        }
    }

    public EpochConsensus(Server member, AtomicInteger epochNumber, int consensusIndex, T valueToBeProposed, AtomicBoolean readPhaseDone) {
        this.member = member;
        this.epochNumber = epochNumber;
        this.consensusIndex = consensusIndex;
        this.readPhaseDone = readPhaseDone;
        this.service = new ExecutorCompletionService<>(member.getExecutor());

        this.state = new ConsensusState<>(new WriteTuple<T>(valueToBeProposed, 0));

        Logger.LOG("Creating epoch consensus");
    }

    public T start() {
        T valueDecided = null;

        while (true) {
            try {
                valueDecided = epoch();
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

                // TODO: just a debug thing
                Logger.ERROR("SHOULD NOT CHANGE EPOCH");

                this.epochNumber.incrementAndGet();

                readPhaseDone.set(false);

                resetCounters();

                continue;
            }
            break;
        }

        return valueDecided;
    }

    public T epoch() throws AbortedSignal {
        Logger.LOG("Starting epoch");

        if (this.state.getMostRecentQuorumWritten().getValue() == null) {
            T valueToBeProposed = member.peekReceivedTx();
            this.state.setMostRecentQuorumWritten(new WriteTuple<>(valueToBeProposed, 0));
        }

        while (true) {
            woa = null;
            // Leader
            if (getLeader(this.epochNumber.get()) == member.getId()) {
                // aborts this epoch if is leader and has no value to propose
                if (this.state.getMostRecentQuorumWritten().getValue() == null) {
                    Logger.LOG("I'm leader and I have no value to propose");
                    abort();
                }

                if (!this.readPhaseDone.get()) {
                    sendToAll(new ReadMessage<>(this.epochNumber.get(), this.consensusIndex));

                    Logger.LOG("send READs done");

                    receiveFromAll(ConsensusMessage.MessageType.STATE);
                    if (woa != null) {
                        break;
                    }

                    Logger.LOG("receive STATEs done");

                    if (Logger.IS_DEBUGGING()) {
                        Logger.DEBUG("Collected states:");
                        System.err.println(this.collected + "\n");
                    }
                }

                this.state.sign(member.getPrivateKey());
                this.collected.addState(member.getId(), this.state);

                sendToAll(new CollectedMessage<>(this.collected, this.epochNumber.get(), this.consensusIndex));

                Logger.LOG("send COLLECTEDs done");
            } else { // Not leader
                boolean wentWrong;
                int leaderId = getLeader(this.epochNumber.get());

                if (!this.readPhaseDone.get()) {
                    wentWrong = !receiveFrom(ConsensusMessage.MessageType.READ, leaderId);
                    if (wentWrong) {
                        Logger.LOG("Received READ went wrong");
                        broadcastAbort();
                        this.woa = checkForEvents();
                    }
                    if (this.woa != null) {
                        break;
                    }

                    Logger.LOG("receive READ done");

                    this.state.sign(member.getPrivateKey());
                    sendTo(new StateMessage<>(this.state, this.epochNumber.get(), this.consensusIndex), leaderId);

                    Logger.LOG("send STATE done");
                }

                wentWrong = !receiveFrom(ConsensusMessage.MessageType.COLLECTED, leaderId);
                if (wentWrong) {
                    Logger.LOG("Received COLLECTED went wrong");
                    broadcastAbort();
                    this.woa = checkForEvents();
                }
                if (this.woa != null) {
                    break;
                }

                Logger.LOG("receive COLLECTED done");

                if (!this.collected.verifyStates(member.getPublicKeys())) {
                    Logger.LOG("Collected states not valid");
                    if (Logger.IS_DEBUGGING()) {
                        Logger.DEBUG("Collected states:");
                        System.err.println(this.collected + "\n");
                    }
                    abort();
                }
            }

            if (Logger.IS_DEBUGGING()) {
                Logger.DEBUG("Collected states:");
                System.err.println(this.collected + "\n");
            }

            T valueToWrite = decideFromCollected();
            sendToAll(new WriteMessage<>(valueToWrite, this.epochNumber.get(), this.consensusIndex));

            Logger.LOG("send WRITEs done");

            writeCounts.inc(valueToWrite, member.getId());
            receiveFromAll(ConsensusMessage.MessageType.WRITE);

            Logger.LOG("receive WRITEs done");

            // if no value to write or accept we can proceed so we abort to a new epoch
            if (this.woa == null) {
                Logger.LOG("No value to write");

                if (Logger.IS_DEBUGGING()) {
                    Logger.DEBUG("Write counts: " + writeCounts);
                    Logger.DEBUG("Accept counts: " + acceptCounts);
                }

                abort();
            }

            break; // end the one iteration loop
        }

        T valueDecided = this.woa.getValue();
        acceptCounts.inc(valueDecided, member.getId());

        sendToAll(new AcceptMessage<>(valueDecided, this.epochNumber.get(), this.consensusIndex));

        Logger.LOG("send ACCEPTs done");

        writeCounts.reset();

        receiveFromAll(ConsensusMessage.MessageType.ACCEPT);

        Logger.LOG("receive ACCEPTs done");

        // if no value to accept we can proceed so we abort to a new epoch
        if (this.woa == null || !this.woa.isToAccept()) {
            Logger.LOG("No value to accept");
            abort();
        }

        return this.woa.getValue();
    }

    public T decideFromCollected() throws AbortedSignal {
        Logger.LOG("Deciding from collected");

        // if the leader didn't send its own state, abort
        if (this.collected.getStates().get(getLeader(this.epochNumber.get())) == null) {
            Logger.LOG("Leader state not found in collected states");
            abort();
        }

        WriteTuple<T> bestWT = null;

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            ConsensusState<T> state = this.collected.getStates().get(i);
            if (state != null) {
                WriteTuple<T> wt = state.getMostRecentQuorumWritten();
                if (bestWT == null
                        || wt.getTimestamp() > bestWT.getTimestamp()) {
                    bestWT = wt;
                }
            }
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
        return this.collected.getStates().get(getLeader(this.epochNumber.get())).getMostRecentQuorumWritten().getValue();
    }

    public int getLeader(int epocNumber) {
        return epocNumber % Config.NUM_MEMBERS;
    }

    public void abort() throws AbortedSignal {
        broadcastAbort();
        throwAbort();
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
                ConsensusMessage<T> message = new NewEpochMessage<>(this.epochNumber.get(), this.consensusIndex);
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
        checkForAborts();

        T value = acceptCounts.getExeeded(2 * Config.ALLOWED_FAILURES);
        if (value != null) {
            Logger.LOG("Quorum of accepts reached: " + value);
            Logger.DEBUG("ac: " + acceptCounts);
            Logger.DEBUG("wc: " + writeCounts);
            return new WritesOrAccepts(value, true);
        }

        value = writeCounts.getExeeded(2 * Config.ALLOWED_FAILURES);
        if (value != null) {
            Logger.LOG("Quorum of writes reached: " + value);
            Logger.DEBUG("ac: " + acceptCounts);
            Logger.DEBUG("wc: " + writeCounts);

            synchronized (this.state) {
                this.state.setMostRecentQuorumWritten(
                        new WriteTuple<T>(value, this.epochNumber.get()));
            }

            return new WritesOrAccepts(value, false);
        }

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

    public void receiveFromAll(ConsensusMessage.MessageType type) throws AbortedSignal {
        List<Future<?>> tasks = new ArrayList<>();
        
        clearTasksQueue();

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i == member.getId()) continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            final int _i = i;
            Future<?> task = this.service.submit(() -> receiveLoop(_i, type));
            tasks.add(task);

            Logger.DEBUG("Task " + i + " started");
        }

        Logger.LOG("Waiting for all tasks to finish");

        // Wait for all threads to finish
        for (int i = 0; i < Config.NUM_MEMBERS-1; i++) {
            try {
                this.service.take();

                Logger.DEBUG((i+1) + " tasks finished");
            } catch (InterruptedException e) {
                Logger.LOG("Thread interrupted: " + e);
            }

            this.woa = checkForEvents();
            if (this.woa != null) {
                Logger.DEBUG("Quorum reached");

                // Cancel all remaining tasks since we've reached a quorum
                for (Future<?> task : tasks) {
                    if (!task.isDone()) {
                        task.cancel(true);
                    }
                }
            }
        }
        
        Logger.LOG("All tasks finished");
    }

    // returns true if nothing went wrong
    public Boolean receiveFrom(ConsensusMessage.MessageType type, int senderId) throws AbortedSignal {
        Future<Boolean> senderTask = null;
        
        List<Future<?>> otherTasks = new ArrayList<>();

        boolean wentWrong = false;

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i == member.getId()) continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            final int _i = i;
            Future<Boolean> future = this.service.submit(() -> receiveLoop(_i, type));
            if (i == senderId) senderTask = future;
            else {
                otherTasks.add(future);
            }
        }

        try {
            // Wait for sender thread to finish
            wentWrong = !senderTask.get();

            if (wentWrong) {
                Logger.LOG("Something went wrong with the sender task");
                abort();
            }
        } catch (ExecutionException | InterruptedException e) {
            Logger.ERROR("Error executing the task: " + e, e);
        }

        for (Future<?> task : otherTasks) {
            if (!task.isDone()) {
                task.cancel(true);
            }
        }

        this.woa = checkForEvents();

        return !wentWrong;
    }


    // returns a boolean indicating if it anything went wrong, useful when
    // communicating with the leader
    public Boolean receiveLoop(int senderId, ConsensusMessage.MessageType type) {
        Logger.DEBUG(senderId + ") Receive loop started for " + type);
        
        int leaderId = getLeader(this.epochNumber.get());

        // Prevents byzantine process to send useless messages to keep the leader stuck
        long startTime = System.currentTimeMillis();

        int timeout = Config.CONSENSUS_LINK_TIMEOUT;
        while (System.currentTimeMillis() - startTime < 2 * timeout) {
            //Logger.DEBUG("Recv loop: time left: " + (2 * Config.LINK_TIMEOUT - (System.currentTimeMillis() - startTime)));
            ConsensusMessage<T> msg = null;
            
            try {
                msg = member.peekConsensusMessageOrWait(senderId, timeout);
            } catch (InterruptedException e) {
                Logger.LOG(senderId + ") Interrupted while peeking consensus message");
                return true;
            } catch (Exception e) {
                Logger.ERROR(senderId + ") Error while peeking consensus message", e);
                return false;
            }
            
            if (msg == null) {
                Logger.LOG(senderId + ") Timed out");
                return false;
            }

            // message from the future: ignore but don't delete
            if (isFromTheFuture(msg)) {
                Logger.LOG(senderId + ") Received message from the future: "
                    + "msg(e=" + msg.getEpochNumber() + ", c=" + msg.getConsensusIndex()
                    + " | act(e=" + this.epochNumber + ", c=" + this.consensusIndex + ")");
                return true;
            }

            member.removeFirstConsensusMessage(senderId);

            // message from the past: ignore and delete
            if (isFromThePast(msg)) {
                Logger.LOG(senderId + ") Received message from the past: "
                    + "msg(e=" + msg.getEpochNumber() + ", c=" + msg.getConsensusIndex()
                    + " | act(e=" + this.epochNumber + ", c=" + this.consensusIndex + ")");
                continue;
            }

            switch (msg.getType()) {
                case READ:
                    Logger.DEBUG(senderId + ") Received READ message");
                    // if not from leader, return
                    if (senderId != leaderId) {
                        Logger.LOG(senderId + ") READ msg not from leader");
                        return false;
                    }

                    // if received READ when not expected, continue
                    if (type != ConsensusMessage.MessageType.READ) {
                        Logger.LOG(senderId + ") " + type + " expected, got READ instead");
                        continue;
                    }

                    return true;

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
                        Logger.DEBUG("Adding state to collected");
                        synchronized (this.collected) {
                            this.collected.addState(senderId, stateMsg.getState());
                        }
                    } else Logger.LOG("Not a valid state");

                    return true;

                case COLLECTED:
                    Logger.DEBUG(senderId + ") Received COLLECTED message");
                    // if not from leader, return
                    if (senderId != leaderId) {
                        Logger.LOG(senderId + ") COLLECTED msg not from leader");
                        return false;
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

                    return true;

                case WRITE:
                    Logger.DEBUG(senderId + ") Received WRITE message");
                    WriteMessage<T> writeMsg = (WriteMessage<T>) msg;

                    writeCounts.inc(writeMsg.getValue(), senderId);

                    synchronized (this.state) {
                        this.state.addToWriteSet(
                                new WriteTuple<T>(writeMsg.getValue(), this.epochNumber.get()));
                    }

                    // if received WRITE when not expected, ignore but continue
                    // because we may have to receive accept message
                    if (type != ConsensusMessage.MessageType.WRITE) {
                        Logger.LOG(senderId + ") " + type + " expected, got WRITE instead");
                        continue;
                    }

                    return true;

                case ACCEPT:
                    Logger.DEBUG(senderId + ") Received ACCEPT message");
                    AcceptMessage<T> acceptMsg = (AcceptMessage<T>) msg;
                    acceptCounts.inc(acceptMsg.getValue(), senderId);

                    if (type != ConsensusMessage.MessageType.ACCEPT) {
                        Logger.LOG(senderId + ") " + type + " expected, got ACCEPT instead");
                    }

                    return true;

                case NEWEPOCH:
                    Logger.LOG(senderId + ") Received new epoch message");

                    abortCounts.inc(senderId);
                    return false;

                default:
                    Logger.LOG("Unknown message type: " + msg.getType());
                    continue;
            }
        }

        Logger.LOG(senderId + ") Big timed out");

        return false;
    }

    private void clearTasksQueue() {
        Future<Boolean> future;
        while ((future = this.service.poll()) != null) {
            future.cancel(true); // Cancels any incomplete tasks
        }
    }
    
    public boolean isFromTheFuture(ConsensusMessage<T> msg) {
        return this.consensusIndex < msg.getConsensusIndex()
                || ( this.consensusIndex == msg.getConsensusIndex()) && this.epochNumber.get() < msg.getEpochNumber();
    }

    public boolean isFromThePast(ConsensusMessage<T> msg) {
        return this.consensusIndex > msg.getConsensusIndex()
                || ( this.consensusIndex == msg.getConsensusIndex()) && this.epochNumber.get() > msg.getEpochNumber();
    }
}
