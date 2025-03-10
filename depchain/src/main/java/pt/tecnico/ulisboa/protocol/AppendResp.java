package pt.tecnico.ulisboa.protocol;

import java.util.Date;

public class AppendResp extends BlockchainMessage {
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private Date timestamp;

    public AppendResp(boolean success, Date timestamp, long seqNum) {
        super(BlockchainMessageType.APPEND_RESP, seqNum);
        this.success = success;
        this.timestamp = timestamp;
    }

    public boolean getSuccess() {
        return success;
    }

    public Date getTimestamp() {
        return timestamp;
    }

}