package pt.tecnico.ulisboa.consensus;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private ConsensusState<T> state;
    private AtomicBoolean readPhaseDone;
    private CollectedStates<T> collected = new CollectedStates<>(Config.NUM_MEMBERS);
    private EventCounter<T> writeCounts = new EventCounter<>();
    private EventCounter<T> acceptCounts = new EventCounter<>();
    private EventCounter<T> abortCounts = new EventCounter<>();
    private boolean hasBroadcastedNewEpoch = false;

    private class WriteOrAccept {
        private T value;
        private boolean accept;

        public WriteOrAccept(T value, boolean accept) {
            this.value = value;
            this.accept = accept;
        }

        public T getValue() {
            return value;
        }

        public boolean isAccept() {
            return accept;
        }
    }

    public EpochConsensus(Node<T> member, int epochNumber, ConsensusState<T> state, AtomicBoolean readPhaseDone) {
        this.member = member;
        this.epochNumber = epochNumber;
        this.state = state;
        this.readPhaseDone = readPhaseDone;

        Logger.LOG("Creating epoch consensus");
    }

    public T start(T valueToBeProposed) throws AbortedSignal {
        Logger.LOG("Starting epoch");

        WriteOrAccept writeOrAccept;

        // Leader
        if (getLeader(this.epochNumber) == member.getId()) {
            // aborts this epoch if is leader and has no value to propose
            if (valueToBeProposed == null) {
                abortAndBroadcast();
            }

            if (!this.readPhaseDone.get()) {
                sendReads();

                receiveStatesOrAbort();
                writeOrAccept = rece

                this.readPhaseDone.set(true);
            }

            this.state.sign(member.getPrivateKey());
            this.collected.addState(member.getId(), this.state);

            sendCollected();
        } else { // Not leader
            receiveReadOrAbort();

            this.state.sign(member.getPrivateKey());
            sendState(this.state);

            receiveCollectedOrAbort();

            if (!this.collected.verifyStates(member.getPublicKeys())) {
                abortAndBroadcast();
            }
        }

        T valueToWrite = decideFromCollected();
        sendWrites(valueToWrite);

        writeCounts.inc(valueToWrite, member.getId());
        T valueDecided = receiveWritesOrAbort();

        sendAccepts(valueDecided);

        acceptCounts.inc(valueDecided, member.getId());
        return receiveAcceptsAndDecideOrAbort();
    }

    public void sendReads() {
        Logger.LOG("Sending read");

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                ConsensusMessage<T> message = new ReadMessage<>(this.epochNumber);
                member.sendToMember(i, message);
            }
        }
    }

    public void sendCollected() {
        Logger.LOG("Sending collected");

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                ConsensusMessage<T> message = new CollectedMessage<>(this.collected, this.epochNumber);
                member.sendToMember(i, message);
            }
        }
    }

    public void sendState(ConsensusState<T> state) {
        Logger.LOG("Sending state");

        int leaderId = getLeader(epochNumber);

        ConsensusMessage<T> message = new StateMessage<>(state, this.epochNumber);

        member.sendToMember(leaderId, message);
    }

    public T decideFromCollected() throws AbortedSignal {
        Logger.LOG("Deciding from collected");

        if (this.collected.getStates().get(getLeader(epochNumber)) == null) {
            abortAndBroadcast();
        }

        // we assume the leader write tuple if no tupple is read
        WriteTuple<T> leaderWT = this.collected.getStates().get(getLeader(epochNumber)).getMostRecentQuorumWritten();

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
            abortAndBroadcast();
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
        return leaderWT.getValue();
    }

    public void sendWrites(T value) {
        Logger.LOG("Sending writes");

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                ConsensusMessage<T> message = new WriteMessage<>(value, this.epochNumber);
                member.sendToMember(i, message);
            }
        }
    }

    public void sendAccepts(T value) {
        Logger.LOG("Sending accepts");

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                ConsensusMessage<T> message = new AcceptMessage<>(value, this.epochNumber);
                member.sendToMember(i, message);
            }
        }
    }

    public int getLeader(int epocNumber) {
        return epocNumber % Config.NUM_MEMBERS;
    }

    public void abort() throws AbortedSignal {
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

    public void abortAndBroadcast() throws AbortedSignal {
        Logger.LOG("Aborting and broadcasting");

        broadcastAbort();
        abort();
    }

    public T receiveFromAll(ConsensusMessage.MessageType type) throws AbortedSignal {
        Thread[] threads = new Thread[Config.NUM_MEMBERS];
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId())
                continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            final int _i = i;
            threads[i] = new Thread(() -> receiveLoop(_i, type));

            threads[i].start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            if (thread != null) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Logger.LOG("Thread interrupted: " + e);
                }
            }
        }

        T value = acceptCounts.getExeeded(2*Config.ALLOWED_FAILURES);
        if (value != null) {
            return value;
        }

        value = writeCounts.getExeeded(2*Config.ALLOWED_FAILURES);
        if (value != null) {

            synchronized(this.state) {
                this.state.setMostRecentQuorumWritten(
                    new WriteTuple<T>(value, this.epochNumber)
                );
            }

            return value;
        }

        if (!hasBroadcastedNewEpoch && abortCounts.exceeded(Config.ALLOWED_FAILURES)) {
            broadcastAbort();
        } else if (abortCounts.exceeded(2*Config.ALLOWED_FAILURES)) {
            abort();
        }

        return null;
    }

    public void receiveFromLeader() throws AbortedSignal {
        int leaderId = getLeader(epochNumber);

        Thread[] threads = new Thread[Config.NUM_MEMBERS];
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId())
                continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            final int _i = i;
            threads[i] = new Thread(() -> receiveLoop(_i, ConsensusMessage.MessageType.READ));

            threads[i].start();
        }

        // In The Future: implement to also keep tracking if the other
        // members send abort messages
        try {
            threads[leaderId].join();
        } catch (InterruptedException e) {
            Logger.LOG("Thread interrupted: " + e);
        }
        
        if (!hasBroadcastedNewEpoch && abortCounts.exceeded(Config.ALLOWED_FAILURES)) {
            broadcastAbort();
        } else if (abortCounts.exceeded(2*Config.ALLOWED_FAILURES)) {
            abort();
        }
    }

    public void receiveLoop(int memberId, ConsensusMessage.MessageType type) {
        int leaderId = getLeader(epochNumber);

        // Prevents byzantine process to send useless messages to keep the leader stuck
        long startTime = System.currentTimeMillis();

        outerloop:
        while(System.currentTimeMillis() - startTime < 2*Config.LINK_TIMEOUT) {
            ConsensusMessage<T> msg = member.pollConsensusMessageOrWait(memberId);

            if (msg == null) {
                Logger.LOG("Timed out");
                break;
            }

            // In The Future: implement synchronization if unsynchronized
            if (msg.getEpochNumber() > this.epochNumber) {
                Logger.LOG("Received message from future epoch");
                break;
            }
            
            if (msg.getEpochNumber() < this.epochNumber) {
                Logger.LOG("Received message from past epoch");
                break;
            }

            switch(msg.getType()) {
                case READ:
                    if (type != ConsensusMessage.MessageType.READ) continue;

                    if (memberId != leaderId) continue;
                    break;
                
                case STATE:
                    if (type != ConsensusMessage.MessageType.STATE) continue;

                    StateMessage<T> stateMsg = (StateMessage<T>) msg;

                    // gotta check if each state received is well signed,
                    // if not dont store it (store null instead)
                    if (stateMsg.getState().verifySignature(member.getPublicKeys().get(memberId))) {
                        synchronized(this.collected) {
                            this.collected.addState(memberId, stateMsg.getState());
                        }
                        return;
                    }
                    break outerloop;

                case COLLECTED:
                    if (type != ConsensusMessage.MessageType.COLLECTED) continue;

                    if (memberId != leaderId) continue;

                    CollectedMessage<T> collectedMsg = (CollectedMessage<T>) msg;
                    this.collected.overwriteWith(collectedMsg.getStates());
                    return;

                case WRITE:
                    WriteMessage<T> writeMsg = (WriteMessage<T>) msg;

                    writeCounts.inc(writeMsg.getValue(), memberId);

                    synchronized(this.state) {
                        this.state.addToWriteSet(
                            new WriteTuple<T>(writeMsg.getValue(), this.epochNumber)
                        );
                    }

                    if (type != ConsensusMessage.MessageType.WRITE) continue;

                    return;

                case ACCEPT:
                    AcceptMessage<T> acceptMsg = (AcceptMessage<T>) msg;
                    acceptCounts.inc(acceptMsg.getValue(), memberId);

                    if (type != ConsensusMessage.MessageType.ACCEPT) continue;

                    return;

                case NEWEPOCH:
                    abortCounts.inc(memberId);
                    return;

                default:
                    continue;
            }
        }
    }
}
