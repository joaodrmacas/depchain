package pt.tecnico.ulisboa.protocol;

public abstract class ClientReq extends BlockchainMessage {
    private static final long serialVersionUID = 1L;

    protected Integer id;
    protected String signature;

    public ClientReq(Integer id, long count) {
        super(BlockchainMessageType.CLIENT_REQ, count);
        this.id = id;
    }

    public abstract ClientReqType getReqType();

    public Integer getId() {
        return id;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    // Enum for request types
    public enum ClientReqType {
        TRANSFER,
        TRANSFER_FROM,
        BLACKLIST,
        APPROVE,
        IS_BLACKLISTED,
        CHECK_BALANCE,
        GET_ALLOWANCE
    }
}