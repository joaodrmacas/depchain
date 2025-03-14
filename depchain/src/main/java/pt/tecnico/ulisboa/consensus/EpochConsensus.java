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
    private boolean readPhaseDone = false;
    private CollectedStates<T> collected = new CollectedStates<>(Config.NUM_MEMBERS);
    private EventCounter<T> writeCounts = new EventCounter<>();
    private EventCounter<T> acceptCounts = new EventCounter<>();
    private EventCounter<T> abortCounts = new EventCounter<>();
    private boolean hasBroadcastedNewEpoch = false;

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

    public EpochConsensus(Node<T> member, int epochNumber, T valueToBeProposed) {
        this.member = member;
        this.epochNumber = epochNumber;

        this.state = new ConsensusState<>(new WriteTuple<T>(valueToBeProposed, epochNumber));

        Logger.LOG("Creating epoch consensus");
    }

    public T start() {
        T valueDecided = null;

        while (true) {
            try {
                epoch();
            } catch (AbortedSignal abs) {
                Logger.LOG("Aborted: " + abs.getMessage());

                // TODO: wait for other aborts til 2*f+1 and then change epoch
                while (true) {
                    break;
                }

                this.epochNumber++;

                readPhaseDone = false;

                continue;
            }
            break;
        }

        return valueDecided;
    }

    public T epoch() throws AbortedSignal {
        Logger.LOG("Starting epoch");

        WritesOrAccepts writesOrAccepts = null;

        T valueToBeProposed = this.state.getMostRecentQuorumWritten().getValue();

        while(true) {
            // Leader
            if (getLeader(this.epochNumber) == member.getId()) {
                // aborts this epoch if is leader and has no value to propose
                if (valueToBeProposed == null) {
                    broadcastAbort();
                }

                if (!this.readPhaseDone) {
                    sendToAll(new ReadMessage<>(this.epochNumber));

                    Logger.LOG("AAAAAAAAAAAA");
                    writesOrAccepts = receiveFromAll(ConsensusMessage.MessageType.STATE);
                    Logger.LOG("BBBBBBBBBBBBBB");
                    if (writesOrAccepts != null) break;

                    this.readPhaseDone = true;
                }

                this.state.sign(member.getPrivateKey());
                this.collected.addState(member.getId(), this.state);

                sendToAll(new CollectedMessage<>(this.collected, this.epochNumber));

            } else { // Not leader
                writesOrAccepts = receiveFrom(ConsensusMessage.MessageType.READ, getLeader(epochNumber));
                if (writesOrAccepts != null) break;

                this.state.sign(member.getPrivateKey());
                sendTo(new StateMessage<>(this.state, this.epochNumber), getLeader(epochNumber));

                writesOrAccepts = receiveFrom(ConsensusMessage.MessageType.COLLECTED, getLeader(epochNumber));
                if (writesOrAccepts != null) break;

                if (!this.collected.verifyStates(member.getPublicKeys())) {
                    broadcastAbort();
                }
            }
            
            T valueToWrite = decideFromCollected();
            sendToAll(new WriteMessage<>(valueToWrite, this.epochNumber));
            
            writeCounts.inc(valueToWrite, member.getId());
            writesOrAccepts = receiveFromAll(ConsensusMessage.MessageType.WRITE);

            // if no value to write or accept we can proceed so we abort to a new epoch
            if (writesOrAccepts == null) {
                broadcastAbort();
                abort();
            }

            break; //end the one iteration loop
        }

        T valueDecided = writesOrAccepts.getValue();
        if (writesOrAccepts.hasQuorumOfWrites()) {
            acceptCounts.inc(valueDecided, member.getId());
        }

        sendToAll(new AcceptMessage<>(valueDecided, this.epochNumber));
        
        writesOrAccepts = receiveFromAll(ConsensusMessage.MessageType.ACCEPT);

        // if no value to write or accept we can proceed so we abort to a new epoch
        if (writesOrAccepts == null) {
            broadcastAbort();
            abort();
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

    public WritesOrAccepts checkForEvents() throws AbortedSignal {
        T value = acceptCounts.getExeeded(2*Config.ALLOWED_FAILURES);
        if (value != null) {
            return new WritesOrAccepts(value, false);
        }

        value = writeCounts.getExeeded(2*Config.ALLOWED_FAILURES);
        if (value != null) {

            synchronized(this.state) {
                this.state.setMostRecentQuorumWritten(
                    new WriteTuple<T>(value, this.epochNumber)
                );
            }

            return new WritesOrAccepts(value, true);
        }

        if (!hasBroadcastedNewEpoch && abortCounts.exceeded(Config.ALLOWED_FAILURES)) {
            broadcastAbort();
        }
        if (abortCounts.exceeded(2*Config.ALLOWED_FAILURES)) {
            abort();
        } 

        return null;
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

            WritesOrAccepts writesOrAccepts = checkForEvents();
            if (writesOrAccepts != null) {
                return writesOrAccepts;
            }
        }

        return null;
    }

    public WritesOrAccepts receiveFrom(ConsensusMessage.MessageType type, int receiverId) throws AbortedSignal {
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

        try {
            threads[receiverId].join();
        } catch (InterruptedException e) {
            Logger.LOG("Thread interrupted: " + e);
        }

        return checkForEvents();
    }

    public void receiveLoop(int senderId, ConsensusMessage.MessageType type) {
        int leaderId = getLeader(epochNumber);

        // Prevents byzantine process to send useless messages to keep the leader stuck
        long startTime = System.currentTimeMillis();

        while(System.currentTimeMillis() - startTime < 2*Config.LINK_TIMEOUT) {
            ConsensusMessage<T> msg = member.peekConsensusMessageOrWait(senderId);

            if (msg == null) {
                Logger.LOG("Timed out");
                break;
            }
            
            // message from the future
            if (msg.getEpochNumber() > this.epochNumber) {
                Logger.LOG("Received message from future epoch");
                break;
            } 

            member.removeFirstConsensusMessage(senderId);

            // message from the past
            if (msg.getEpochNumber() < this.epochNumber) {
                Logger.LOG("Received message from past epoch");
                continue;
            }
            
            switch(msg.getType()) {
                case READ:
                    // if not from leader, return
                    if (senderId != leaderId) return;

                    // if received READ when not expected, return
                    if (type != ConsensusMessage.MessageType.READ) return;

                    break;
                
                case STATE:
                    // if received STATE when not expected, return
                    if (type != ConsensusMessage.MessageType.STATE) return;

                    StateMessage<T> stateMsg = (StateMessage<T>) msg;

                    // gotta check if each state received is well signed,
                    // if not dont store it (store null instead)
                    if (stateMsg.getState().verifySignature(member.getPublicKeys().get(senderId))) {
                        synchronized(this.collected) {
                            this.collected.addState(senderId, stateMsg.getState());
                        }
                    }

                    return;

                case COLLECTED:                    
                    // if not from leader, return
                    if (senderId != leaderId) return;
                    
                    // if received COLLECTED when not expected, return
                    if (type != ConsensusMessage.MessageType.COLLECTED) return;

                    CollectedMessage<T> collectedMsg = (CollectedMessage<T>) msg;
                    synchronized(this.collected) {
                        this.collected.overwriteWith(collectedMsg.getStates());
                    }
                    return;

                case WRITE:
                    WriteMessage<T> writeMsg = (WriteMessage<T>) msg;

                    writeCounts.inc(writeMsg.getValue(), senderId);

                    synchronized(this.state) {
                        this.state.addToWriteSet(
                            new WriteTuple<T>(writeMsg.getValue(), this.epochNumber)
                        );
                    }

                    // if received WRITE when not expected, ignore but continue 
                    // because we may have to receive accept message
                    if (type != ConsensusMessage.MessageType.WRITE) continue;

                    return;

                case ACCEPT:
                    AcceptMessage<T> acceptMsg = (AcceptMessage<T>) msg;
                    acceptCounts.inc(acceptMsg.getValue(), senderId);

                    return;

                case NEWEPOCH:
                    abortCounts.inc(senderId);
                    return;

                default:
                    continue;
            }
        }
    }
}
