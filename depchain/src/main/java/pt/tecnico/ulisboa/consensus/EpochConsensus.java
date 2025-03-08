package pt.tecnico.ulisboa.consensus;

import java.util.List;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.ReadMessage;
import pt.tecnico.ulisboa.network.AuthenticatedPerfectLink;
import pt.tecnico.ulisboa.utils.Logger;

public class EpochConsensus<T> {
    private AuthenticatedPerfectLink link;
    private int memberId;
    private int epochNumber;
    private T value;
    private ConsensusState<T> state;
    private Boolean readPhaseDone;

    public EpochConsensus(AuthenticatedPerfectLink link, int memberId, int epochNumber, ConsensusState<T> state, Boolean readPhaseDone) {
        this.link = link;
        this.memberId = memberId;
        this.epochNumber = epochNumber;
        this.state = state;
        this.readPhaseDone = readPhaseDone;

        Logger.LOG("Creating epoch consensus");
    }

    public T start(T valueToBeProposed) throws AbortedSignal {
        Logger.LOG("Starting epoch");

        CollectedStates<T> collected;

        if (getLeader(epochNumber) == memberId) {
            if (!readPhaseDone) {
                sendReads();

                collected = receiveStatesOrAbort();

                collected.verifyStates();

                collected.addState(this.memberId, this.state);

                readPhaseDone = true;
            }



        } else {
            ;
        }

        return value;
    }

    public void sendReads() {
        Logger.LOG("Sending read");

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != memberId) {
                ConsensusMessage<T> message = new ReadMessage<>();
                link.send(memberId, message);
            }
        }
    }

    public CollectedStates<T> receiveStatesOrAborts() throws AbortedSignal {
        CollectedStates<T> collected = new CollectedStates<>(Config.NUM_MEMBERS);

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i != memberId) continue;
            Thread t = new Thread(() -> {
                link.receiveFrom(i);
            });
        }
    }

    public void endEpoch() {
        Logger.LOG("Ending epoch");
    }

    public void sendWrite(T value) {
        Logger.LOG("Sending write: " + value);
    }

    public void sendAccept(T value) {
        Logger.LOG("Sending accept: " + value);
    }

    public int getLeader(int epocNumber) {
        return epocNumber % Config.NUM_MEMBERS;
    }
}
