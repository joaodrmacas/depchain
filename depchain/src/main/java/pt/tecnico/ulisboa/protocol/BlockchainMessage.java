package pt.tecnico.ulisboa.protocol;

import java.io.Serializable;

import pt.tecnico.ulisboa.utils.RequiresEquals;

public abstract class BlockchainMessage implements RequiresEquals, Serializable {

    private static final long serialVersionUID = 1L;
    private BlockchainMessageType type;
    private long seqNum;


    public BlockchainMessage(BlockchainMessageType type, long seqNum) {
        this.type = type;
    }

    public static enum BlockchainMessageType {
        APPEND_REQ,
        APPEND_RESP,
        KEY_REGISTER_REQ,
    }

    public BlockchainMessageType getType() {
        return type;
    }

    public long getSeqNum() {
        return seqNum;
    }
    
}