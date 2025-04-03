package pt.tecnico.ulisboa.consensus.message;

import java.io.Serializable;

public abstract class ConsensusMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private MessageType type;
    protected int epochNumber;
    protected int consensusIndex;

    public ConsensusMessage(MessageType type, int epochNumber, int consensusIndex) {
        this.type = type;
        this.epochNumber = epochNumber;
        this.consensusIndex = consensusIndex;
    }

    public static enum MessageType {
        READ,
        STATE,
        COLLECTED,
        WRITE,
        ACCEPT,
        NEWEPOCH,
        DUMMY,
    }

    public MessageType getType() {
        return type;
    }

    public int getEpochNumber() {
        return epochNumber;
    }

    public int getConsensusIndex() {
        return consensusIndex;
    }

    public String toString() {
        return "ConsensusMessage(" + type + ", " + epochNumber +  ", " + consensusIndex + ")";
    }
}
