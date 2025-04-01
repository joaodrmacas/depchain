package pt.tecnico.ulisboa.protocol;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ClientResp that = (ClientResp) o;
        return success == that.success && Objects.equals(seqNum, that.seqNum) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, seqNum, message);
    }
}
