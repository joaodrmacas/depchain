package pt.tecnico.ulisboa.protocol;

import java.time.LocalDateTime;

public class ClientResp extends BlockchainMessage {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private Long seqNum;
    private String message;

    public ClientResp(boolean success, Long seqNum) {
        super(BlockchainMessageType.CLIENT_RESP, seqNum);
        this.success = success;
        this.message = "No message provided";
    }

    public ClientResp(boolean success, Long seqNum, String message) {
        super(BlockchainMessageType.CLIENT_RESP, seqNum);
        this.success = success;
        this.message = message;
    }

    public boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

}