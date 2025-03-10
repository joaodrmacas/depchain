package pt.tecnico.ulisboa.protocol;

import java.util.Date;

public class BlockchainResponse extends BlockchainMessage {
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private Date timestamp;
    private long seqNum;

    public BlockchainResponse(boolean success, Date timestamp) {
        super(MessageType.BLOCKCHAIN_RESP);
        this.success = success;
        this.timestamp = timestamp;
    }

    public boolean getSuccess() {
        return success;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public long getSeqNum() {
        return seqNum;
    }

}