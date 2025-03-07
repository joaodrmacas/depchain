package pt.tecnico.ulisboa.consensus;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.AuthenticatedPerfectLink;

public class EpochConsensus<T> {
    private AuthenticatedPerfectLink link;
    private int processId;
    private int epochNumber;
    private T value;
    private ConsensusState<T> state;
    private Boolean readPhaseDone;

    public enum MESSAGE {
        READ,
        STATE,
        COLLECTED,
        WRITE,
        ACCEPT,
    }

    public EpochConsensus(AuthenticatedPerfectLink link, int processId, int epochNumber, ConsensusState<T> state, Boolean readPhaseDone) {
        this.link = link;
        this.processId = processId;
        this.epochNumber = epochNumber;
        this.state = state;
        this.readPhaseDone = readPhaseDone;

        System.out.println("Creating epoch consensus");
    }

    public T start() throws AbortedSignal {
        System.out.println("Starting epoch");

        if (getLeader(epochNumber) == processId) {
            if (!readPhaseDone) {
                for (int i = 0; i < Config.NUM_PROCESSES; i++) {
                    if (i != processId) {
                        sendRead();
                    }
                }

                // waitForStates();
            }

        } else {
            sendRead();
        }

        return value;
    }

    public void endEpoch() {
        System.out.println("Ending epoch");
    }

    public void sendWrite(T value) {
        System.out.println("Sending write: " + value);
    }

    public void sendRead() {
        System.out.println("Sending read");
    }

    public void sendAccept(T value) {
        System.out.println("Sending accept: " + value);
    }

    public int getLeader(int epocNumber) {
        return epocNumber % Config.NUM_PROCESSES;
    }
}
