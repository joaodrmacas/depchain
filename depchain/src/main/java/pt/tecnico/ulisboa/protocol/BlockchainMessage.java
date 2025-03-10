package pt.tecnico.ulisboa.protocol;

import java.io.Serializable;

import pt.tecnico.ulisboa.utils.RequiresEquals;

public abstract class BlockchainMessage<T> implements RequiresEquals, Serializable {

    private static final long serialVersionUID = 1L;
    private MessageType type;

    public BlockchainMessage(MessageType type) {
        this.type = type;
    }

    public static enum MessageType {
        CLIENT_REQUEST,
        CLIENT_RESPONSE,
    }

    public MessageType getType() {
        return type;
    }
    
}