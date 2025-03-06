package pt.tecnico.ulisboa.consensus;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.AuthenticatedPerfectLink;

public class EpochConsensus<T> {
    private AuthenticatedPerfectLink link;
    private int processId;
    private int epochNumber;
    private T value;
    private ConsensusState<T> state;
    private int index;

    public enum MESSAGE {
        READ,
        STATE,
        COLLECTED,
        WRITE,
        ACCEPT,
    }

    public EpochConsensus(int index, AuthenticatedPerfectLink link, int processId, int epochNumber, ConsensusState<T> state) {
        this.link = link;
        this.processId = processId;
        this.epochNumber = epochNumber;
        this.state = state;
        this.index = index;

        System.out.println("Creating epoch consensus");
    }

    public T start() throws AbortedSignal, UnsynchronizedNodeException {
        System.out.println("Starting epoch");

        if (getLeader(epochNumber) == processId) {
            sendWrite(value);
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
