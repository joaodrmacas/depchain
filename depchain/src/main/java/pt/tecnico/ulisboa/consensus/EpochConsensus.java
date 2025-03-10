package pt.tecnico.ulisboa.consensus;

import java.util.Map;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.consensus.message.AcceptMessage;
import pt.tecnico.ulisboa.consensus.message.CollectedMessage;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.NewEpoch;
import pt.tecnico.ulisboa.consensus.message.ReadMessage;
import pt.tecnico.ulisboa.consensus.message.WriteMessage;
import pt.tecnico.ulisboa.node.Node;
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
        }
        else { //Not leader
            receiveReadOrAbort();

            this.state.sign(member.getPrivateKey());
            sendState(this.state);

            collected = receiveCollectedOrAbort();

            if (!collected.verifyStates(member.getPublicKeys())) {
                abort();
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
                ConsensusMessage<T> message =
                    new ReadMessage<>(this.epochNumber);
                member.getLink(i).send(message);
            }
        }
    }

    // TODO
    public CollectedStates<T> receiveStatesOrAbort() throws AbortedSignal {
        CollectedStates<T> collected = new CollectedStates<>(Config.NUM_MEMBERS);

        // TODO: gotta check if each state received is well signed,
        // if not dont store it (store null instead)
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            new Thread( () -> {

            }).start();
        }

        return collected;
    }

    public void sendCollected(CollectedStates<T> collected) {
        Logger.LOG("Sending collected");

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                ConsensusMessage<T> message =
                    new CollectedMessage<>(collected, this.epochNumber);
                member.getLink(i).send(message);
            }
        }
    }

    // TODO
    public void receiveReadOrAbort() throws AbortedSignal {
        int leaderId = getLeader(epochNumber);

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            new Thread( () -> {
                if (i == leaderId) {
                    // Receive the read and abort the other threads
                } else {
                    // Interpret the message and ignore
                    // or send abort and abort the other threads
                }
            }).start();
        }
    }

    public void sendState(ConsensusState<T> state) {
        Logger.LOG("Sending state");

        int leaderId = getLeader(epochNumber);

        member.getLink(leaderId).send(state);
    }

    // TODO
    public CollectedStates<T> receiveCollectedOrAbort() throws AbortedSignal {
        CollectedStates<T> collected = null;
        int leaderId = getLeader(epochNumber);

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) continue;

            // Thread that listens to each link
            // interpreting each message receive and deciding after
            new Thread( () -> {
                if (i == leaderId) {
                    // Receive the collected states and abort the other threads
                } else {
                    // Interpret the message and ignore
                    // or send abort and abort the other threads
                }
            }).start();
        }

        return collected;
    }

    public T decideFromCollected(CollectedStates<T> collected) throws AbortedSignal {
        Logger.LOG("Deciding from collected");

        if (collected.getStates().get(getLeader(epochNumber)) == null) {
            abort();
        }

        // TODO: dont know if is right to assume the leader wt
        WriteTuple<T> leaderWT =
            collected.getStates().get(getLeader(epochNumber)).getMostRecentQuorumWritten();

        WriteTuple<T> bestWT= null;

        for (int i=0; i < Config.NUM_MEMBERS; i++) {
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
            for (int i=0; i < Config.NUM_MEMBERS; i++) {
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

    // TODO
    public T receiveWritesOrAbort() {
        Logger.LOG("Receiving writes");

        // Waits for writes till it reaches a reasonable number
        // of equal writes
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                // Thread that listens to each link
                // interpreting each message receive and deciding after
                new Thread( () -> {

                }).start();
            }
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

    public T receiveAcceptsAndDecideOrAbort() {
        Logger.LOG("Receiving accepts");

        // Waits for accepts till it reaches a reasonable number
        // of equal accepts
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                // Thread that listens to each link
                // interpreting each message receive and deciding after
                new Thread( () -> {

                }).start();
            }
        }

        return null;
    }

    public int getLeader(int epocNumber) {
        return epocNumber % Config.NUM_MEMBERS;
    }

    public void abort() throws AbortedSignal {
        Logger.LOG("Aborting");

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != member.getId()) {
                ConsensusMessage<T> message = new NewEpoch<>(this.epochNumber);
                member.getLink(i).send(message);
            }
        }

        throw new AbortedSignal("Aborted");
    }
}
