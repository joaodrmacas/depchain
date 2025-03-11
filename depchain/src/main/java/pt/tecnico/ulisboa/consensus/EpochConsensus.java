package pt.tecnico.ulisboa.consensus;

import java.util.Map;

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
    private Boolean readPhaseDone;
    private EventCounter<T> writeCounts = new EventCounter<>();
    private EventCounter<T> acceptCounts = new EventCounter<>();
    private EventCounter<T> abortCounts = new EventCounter<>();
    private boolean hasBroadcastedNewEpoch = false;

    public EpochConsensus(Node<T> member, int epochNumber, ConsensusState<T> state, Boolean readPhaseDone) {
        this.member = member;
        this.epochNumber = epochNumber;
        this.state = state;
        this.readPhaseDone = readPhaseDone;

        Logger.LOG("Creating epoch consensus");
    }

    public T start(T valueToBeProposed) throws AbortedSignal {
        Logger.LOG("Starting epoch");

        CollectedStates<T> collected;

        // Leader
        if (getLeader(this.epochNumber) == member.getId()) {
            if (!this.readPhaseDone) {
                sendReads();

                collected = receiveStatesOrAbort();

                this.readPhaseDone = true;
            } else {
                collected = new CollectedStates<>(Config.NUM_MEMBERS);
            }

            this.state.sign(member.getPrivateKey());
            collected.addState(member.getId(), this.state);

            sendCollected(collected);
        } else { // Not leader
            receiveReadOrAbort();

            this.state.sign(member.getPrivateKey());
            sendState(this.state);

            collected = receiveCollectedOrAbort();

            if (!collected.verifyStates(member.getPublicKeys())) {
                synchronized(abortCounts) {
                    abortAndBroadcast();
                }
            }
        }

        T valueToWrite = decideFromCollected(collected);
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
                member.getLink(i).send(message);
            }
        }
    }

    public CollectedStates<T> receiveStatesOrAbort() throws AbortedSignal {
        CollectedStates<T> collected = new CollectedStates<>(Config.NUM_MEMBERS);

        Thread[] threads = new Thread[Config.NUM_MEMBERS];
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId())
                continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            final int _i = i;
            threads[i] = new Thread(() -> {
                boolean done = true;
                while(true) {
                    ConsensusMessage<T> msg = member.fetchConsensusMessageOrWait(_i);

                    if (msg == null) {
                        Logger.LOG("Timeouted");
                        break;
                    }

                    // In The Future: implement synchronization if unsynchronized
                    if (msg.getEpochNumber() > this.epochNumber) {
                        Logger.LOG("Received message from future epoch");
                        break;
                    } else if (msg.getEpochNumber() < this.epochNumber) {
                        Logger.LOG("Received message from past epoch");
                        continue;
                    }

                    switch(msg.getType()) {
                        case STATE:
                            StateMessage<T> stateMsg = (StateMessage<T>) msg;

                            // gotta check if each state received is well signed,
                            // if not dont store it (store null instead)
                            if (stateMsg.getState().verifySignature(member.getPublicKeys().get(_i))) {
                                synchronized(collected) {
                                    collected.addState(_i, stateMsg.getState());
                                }
                                return;
                            }
                            break;
                        case NEWEPOCH:
                            synchronized(abortCounts) {
                                abortCounts.inc(_i);
                            }
                            break;
                        default:
                            done = false;
                            break;
                    }
                    if (!done) continue;
                }

                synchronized(collected) {
                    collected.addState(_i, null);
                }
            });

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

            synchronized(abortCounts) {
                if (!hasBroadcastedNewEpoch && abortCounts.exceeded(Config.ALLOWED_FAILURES)) {
                    broadcastAbort();
                } else if (abortCounts.exceeded(2*Config.ALLOWED_FAILURES)) {
                    abort();
                }
            }
        }

        return collected;
    }

    public void sendCollected(CollectedStates<T> collected) {
        Logger.LOG("Sending collected");

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                ConsensusMessage<T> message = new CollectedMessage<>(collected, this.epochNumber);
                member.getLink(i).send(message);
            }
        }
    }

    public void receiveReadOrAbort() throws AbortedSignal {
        int leaderId = getLeader(epochNumber);

        Thread[] threads = new Thread[Config.NUM_MEMBERS];
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId())
                continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            final int _i = i;
            threads[i] = new Thread(() -> {
                boolean done = true;
                while(true) {
                    ConsensusMessage<T> msg = member.fetchConsensusMessageOrWait(_i);

                    if (msg == null) {
                        Logger.LOG("Timeouted");
                        break;
                    }

                    // In The Future: implement synchronization if unsynchronized
                    if (msg.getEpochNumber() > this.epochNumber) {
                        Logger.LOG("Received message from future epoch");
                        break;
                    } else if (msg.getEpochNumber() < this.epochNumber) {
                        Logger.LOG("Received message from past epoch");
                        continue;
                    }

                    switch(msg.getType()) {
                        case READ:
                            if (_i != leaderId) continue;
                            break;
                        case NEWEPOCH:
                            synchronized(abortCounts) {
                                abortCounts.inc(_i);
                            }
                            break;
                        default:
                            done = false;
                            break;
                    }
                    if (!done) continue;
                }
            });

            threads[i].start();
        }

        // In The Future: implement to also keep tracking if the other
        // members send abort messages
        try {
            threads[leaderId].join();
        } catch (InterruptedException e) {
            Logger.LOG("Thread interrupted: " + e);
        }
        
        synchronized(abortCounts) {
            if (!hasBroadcastedNewEpoch && abortCounts.exceeded(Config.ALLOWED_FAILURES)) {
                broadcastAbort();
            } else if (abortCounts.exceeded(2*Config.ALLOWED_FAILURES)) {
                abort();
            }
        }
    }

    public void sendState(ConsensusState<T> state) {
        Logger.LOG("Sending state");

        int leaderId = getLeader(epochNumber);

        member.getLink(leaderId).send(state);
    }

    // TODO
    public CollectedStates<T> receiveCollectedOrAbort() throws AbortedSignal {
        CollectedStates<T> collected = new CollectedStates<>(Config.NUM_MEMBERS);
        int leaderId = getLeader(epochNumber);

        Thread[] threads = new Thread[Config.NUM_MEMBERS];
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId())
                continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            final int _i = i;
            threads[i] = new Thread(() -> {
                boolean done = true;
                while(true) {
                    ConsensusMessage<T> msg = member.fetchConsensusMessageOrWait(_i);

                    if (msg == null) {
                        Logger.LOG("Timeouted");
                        break;
                    }

                    // In The Future: implement synchronization if unsynchronized
                    if (msg.getEpochNumber() > this.epochNumber) {
                        Logger.LOG("Received message from future epoch");
                        break;
                    } else if (msg.getEpochNumber() < this.epochNumber) {
                        Logger.LOG("Received message from past epoch");
                        break;
                    }

                    switch(msg.getType()) {
                        case COLLECTED:
                            if (_i != leaderId) continue;

                            CollectedMessage<T> collectedMsg = (CollectedMessage<T>) msg;
                            collected.overwriteWith(collectedMsg.getStates());

                            break;
                        case NEWEPOCH:
                            synchronized(abortCounts) {
                                abortCounts.inc(_i);
                            }
                            break;
                        default:
                            done = false;
                            break;
                    }
                    if (!done) continue;
                }
            });

            threads[i].start();
        }

        // In The Future: implement to also keep tracking if the other
        // members send abort messages
        try {
            threads[leaderId].join();
        } catch (InterruptedException e) {
            Logger.LOG("Thread interrupted: " + e);
        }
        
        synchronized(abortCounts) {
            if (!hasBroadcastedNewEpoch && abortCounts.exceeded(Config.ALLOWED_FAILURES)) {
                broadcastAbort();
            } else if (abortCounts.exceeded(2*Config.ALLOWED_FAILURES)) {
                abort();
            }
        }

        return collected;
    }

    public T decideFromCollected(CollectedStates<T> collected) throws AbortedSignal {
        Logger.LOG("Deciding from collected");

        if (collected.getStates().get(getLeader(epochNumber)) == null) {
            synchronized(abortCounts) {
                abortAndBroadcast();
            }
        }

        // we assume the leader write tuple if no tupple is read
        WriteTuple<T> leaderWT = collected.getStates().get(getLeader(epochNumber)).getMostRecentQuorumWritten();

        WriteTuple<T> bestWT = null;

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            ConsensusState<T> state = collected.getStates().get(i);
            if (state != null) {
                WriteTuple<T> wt = state.getMostRecentQuorumWritten();
                if (bestWT == null
                        || wt.getTimestamp() < bestWT.getTimestamp()) {
                    bestWT = wt;
                }
            }
        }

        if (bestWT != null) {
            int count = 0;
            for (int i = 0; i < Config.NUM_MEMBERS; i++) {
                ConsensusState<T> state = collected.getStates().get(i);
                if (state != null) {
                    Map<T, WriteTuple<T>> ws = state.getWriteSet();
                    for (T value : ws.keySet()) {
                        if (bestWT.getValue().equals(value)) {
                            count++;
                            if (count >= Config.ALLOWED_FAILURES + 1) {
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
                member.getLink(i).send(message);
            }
        }
    }

    public T receiveWritesOrAbort() throws AbortedSignal {
        Logger.LOG("Receiving writes");

        Thread[] threads = new Thread[Config.NUM_MEMBERS];
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId())
                continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            final int _i = i;
            threads[i] = new Thread(() -> {
                boolean done = true;
                while(true) {
                    ConsensusMessage<T> msg = member.fetchConsensusMessageOrWait(_i);

                    if (msg == null) {
                        Logger.LOG("Timeouted");
                        break;
                    }

                    // In The Future: implement synchronization if unsynchronized
                    if (msg.getEpochNumber() > this.epochNumber) {
                        Logger.LOG("Received message from future epoch");
                        break;
                    } else if (msg.getEpochNumber() < this.epochNumber) {
                        Logger.LOG("Received message from past epoch");
                        continue;
                    }

                    switch(msg.getType()) {
                        case WRITE:
                            WriteMessage<T> writeMsg = (WriteMessage<T>) msg;

                            synchronized(writeCounts) {
                                writeCounts.inc(writeMsg.getValue(), _i);
                            }

                            synchronized(this.state) {
                                this.state.addToWriteSet(
                                    new WriteTuple<T>(writeMsg.getValue(), writeMsg.getEpochNumber())
                                );
                            }

                            break;
                        case NEWEPOCH:
                            synchronized(abortCounts) {
                                abortCounts.inc(_i);
                            }
                            break;
                        default:
                            done = false;
                            break;
                    }
                    if (!done) continue;
                }
            });

            threads[i].start();
        }

        // Wait for all threads to finish or till a quorum is reached
        for (Thread thread : threads) {
            if (thread != null) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Logger.LOG("Thread interrupted: " + e);
                }
            }
            synchronized(writeCounts) {
                T value = writeCounts.getExeeded(2*Config.ALLOWED_FAILURES);
                if (value != null) {

                    synchronized(this.state) {
                        this.state.setMostRecentQuorumWritten(
                            new WriteTuple<T>(value, this.epochNumber)
                        );
                    }

                    return value;
                }
            }

            synchronized(abortCounts) {
                if (!hasBroadcastedNewEpoch && abortCounts.exceeded(Config.ALLOWED_FAILURES)) {
                    broadcastAbort();
                } else if (abortCounts.exceeded(2*Config.ALLOWED_FAILURES)) {
                    abort();
                }
            }
        }

        synchronized(abortCounts) {
            abortAndBroadcast();
        }

        return null;
    }

    public void sendAccepts(T value) {
        Logger.LOG("Sending accepts");

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                ConsensusMessage<T> message = new AcceptMessage<>(value, this.epochNumber);
                member.getLink(i).send(message);
            }
        }
    }

    // TODO
    public T receiveAcceptsAndDecideOrAbort() throws AbortedSignal {
        Logger.LOG("Receiving accepts");

        Thread[] threads = new Thread[Config.NUM_MEMBERS];
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId())
                continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            final int _i = i;
            threads[i] = new Thread(() -> {
                boolean done = true;
                while(true) {
                    ConsensusMessage<T> msg = member.fetchConsensusMessageOrWait(_i);

                    if (msg == null) {
                        Logger.LOG("Timeouted");
                        break;
                    }

                    // In The Future: implement synchronization if unsynchronized
                    if (msg.getEpochNumber() > this.epochNumber) {
                        Logger.LOG("Received message from future epoch");
                        break;
                    } else if (msg.getEpochNumber() < this.epochNumber) {
                        Logger.LOG("Received message from past epoch");
                        continue;
                    }

                    switch(msg.getType()) {
                        case ACCEPT:
                            AcceptMessage<T> acceptMsg = (AcceptMessage<T>) msg;

                            synchronized(acceptCounts) {
                                acceptCounts.inc(acceptMsg.getValue(), _i);
                            }

                            break;
                        case NEWEPOCH:
                            synchronized(abortCounts) {
                                abortCounts.inc(_i);
                            }
                            break;
                        default:
                            done = false;
                            break;
                    }
                    if (!done) continue;
                }
            });

            threads[i].start();
        }

        // Wait for all threads to finish or till a quorum is reached
        for (Thread thread : threads) {
            if (thread != null) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Logger.LOG("Thread interrupted: " + e);
                }
            }
            synchronized(acceptCounts) {
                T value = acceptCounts.getExeeded(2*Config.ALLOWED_FAILURES);
                if (value != null) {
                    return value;
                }
            }

            synchronized(abortCounts) {
                if (!hasBroadcastedNewEpoch && abortCounts.exceeded(Config.ALLOWED_FAILURES)) {
                    broadcastAbort();
                } else if (abortCounts.exceeded(2*Config.ALLOWED_FAILURES)) {
                    abort();
                }
            }
        }

        synchronized(abortCounts) {
            abortAndBroadcast();
        }

        return null;
    }

    public int getLeader(int epocNumber) {
        return epocNumber % Config.NUM_MEMBERS;
    }

    public void abort() throws AbortedSignal {
        Logger.LOG("Aborting");
        
        throw new AbortedSignal("Aborted");
    }

    // always use this within a synchronized block
    public void broadcastAbort() {
        Logger.LOG("Broadcasting abort");

        abortCounts.inc(member.getId());

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                ConsensusMessage<T> message = new NewEpochMessage<>(this.epochNumber);
                member.getLink(i).send(message);
            }
        }

        hasBroadcastedNewEpoch = true;
    }

    // always use this within a synchronized block
    public void abortAndBroadcast() throws AbortedSignal {
        Logger.LOG("Aborting and broadcasting");

        broadcastAbort();
        abort();
    }
}
