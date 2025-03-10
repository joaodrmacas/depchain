package pt.tecnico.ulisboa.protocol;

import java.io.Serializable;

import pt.tecnico.ulisboa.utils.RequiresEquals;

public abstract class BlockchainMessage implements RequiresEquals, Serializable {

    private static final long serialVersionUID = 1L;
    private MessageType type;

    public BlockchainMessage(MessageType type) {
        this.type = type;
    }

    public static enum MessageType {
        BLOCKCHAIN_REQ,
        BLOCKCHAIN_RESP,
        CLIENT_KEY_REGISTER,
    }

    public MessageType getType() {
        return type;
    }
    
}