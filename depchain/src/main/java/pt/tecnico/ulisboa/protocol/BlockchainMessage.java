package pt.tecnico.ulisboa.protocol;

import java.io.Serializable;

import pt.tecnico.ulisboa.utils.RequiresEquals;

public abstract class BlockchainMessage implements RequiresEquals, Serializable {

    private static final long serialVersionUID = 1L;
    private BlockchainMessageType type;
    private Long count;


    public BlockchainMessage(BlockchainMessageType type, long count) {
        this.type = type;
        this.count = count;
    }

    public static enum BlockchainMessageType {
        APPEND_REQ,
        APPEND_RESP,
        REGISTER_REQ,
        REGISTER_RESP,
    }

    public BlockchainMessageType getType() {
        return type;
    }

    public Long getCount() {
        return count;
    }

    public int getSenderId(){
        return -1; //TODO: isto está insanely disgusting mas tenho cpd para fazer - fix this 
    }
    
}