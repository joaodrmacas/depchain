package pt.tecnico.ulisboa.protocol;

import java.time.LocalDateTime;

public class ClientResp extends BlockchainMessage {
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private LocalDateTime timestamp;

    public ClientResp(boolean success, LocalDateTime timestamp) {
        super(BlockchainMessageType.CLIENT_RESP, 1); //TDIsgusting
        this.success = success;
        this.timestamp = timestamp;
    }

    public ClientResp(boolean success, LocalDateTime timestamp, long seqNum) {
        super(BlockchainMessageType.CLIENT_RESP, seqNum);
        this.success = success;
        this.timestamp = timestamp;
    }

    public boolean getSuccess() {
        return success;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

}