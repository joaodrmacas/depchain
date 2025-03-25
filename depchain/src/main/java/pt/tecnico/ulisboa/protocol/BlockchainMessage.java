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
        CLIENT_REQ,
        CLIENT_RESP,
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
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockchainMessage) {
            BlockchainMessage other = (BlockchainMessage) obj;
            return type.equals(other.type) && count.equals(other.count);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + type.hashCode();
        result = 31 * result + count.hashCode();
        return result;
    }
}