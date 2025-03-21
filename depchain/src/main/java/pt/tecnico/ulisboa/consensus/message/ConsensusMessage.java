package pt.tecnico.ulisboa.consensus.message;

import java.io.Serializable;

import pt.tecnico.ulisboa.utils.RequiresEquals;

public abstract class ConsensusMessage<T extends RequiresEquals> implements Serializable {
    private static final long serialVersionUID = 1L;
    private MessageType type;
    protected int epochNumber;

    public ConsensusMessage(MessageType type, int epochNumber) {
        this.type = type;
        this.epochNumber = epochNumber;
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

    public String toString() {
        return "ConsensusMessage(" + type + ", " + epochNumber + ")";
    }
}
