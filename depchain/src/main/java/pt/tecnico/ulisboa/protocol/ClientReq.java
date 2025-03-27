package pt.tecnico.ulisboa.protocol;

import java.time.LocalDateTime;

public abstract class ClientReq extends BlockchainMessage {
    private static final long serialVersionUID = 1L;

    protected Integer id;
    protected String signature;
    protected LocalDateTime timestamp;

    public ClientReq(Integer id, String signature, long count) {
        super(BlockchainMessageType.CLIENT_REQ, count);
        this.id = id;
        this.signature = signature;
        this.timestamp = LocalDateTime.now();
    }

    public abstract ClientReqType getReqType();

    public Integer getId() {
        return id;
    }

    public String getSignature() {
        return signature;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // Enum for request types
    public enum ClientReqType {
        TRANSFER,
        TRANSFER_FROM,
        BLACKLIST,
        APPROVE,
        APPEND_REQ // TODO: this one should be deleted
    }
}