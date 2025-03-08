package pt.tecnico.ulisboa.consensus.message;

import java.io.Serializable;

public class ConsensusMessage<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private MessageType type;

    public ConsensusMessage(MessageType type) {
        this.type = type;
    }

    public static enum MessageType {
        READ,
        STATE,
        COLLECTED,
        WRITE,
        ACCEPT,
    }

    public MessageType getType() {
        return type;
    }
}
