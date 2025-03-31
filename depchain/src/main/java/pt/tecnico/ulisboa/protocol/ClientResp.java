package pt.tecnico.ulisboa.protocol;

import java.time.LocalDateTime;

public class ClientResp extends BlockchainMessage {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private LocalDateTime timestamp;
    private String message;

    public ClientResp(boolean success, LocalDateTime timestamp) {
        super(BlockchainMessageType.CLIENT_RESP, Long.valueOf(1)); // TODO: TDIsgusting
        this.success = success;
        this.timestamp = timestamp;
        this.message = "No message provided";
    }

    public ClientResp(boolean success, LocalDateTime timestamp, String message) {
        super(BlockchainMessageType.CLIENT_RESP, Long.valueOf(1)); // TODO: TDIsgusting
        this.success = success;
        this.timestamp = timestamp;
        this.message = message;
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

    public String getMessage() {
        return message;
    }

}