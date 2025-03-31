package pt.tecnico.ulisboa.protocol;

public abstract class ClientReq extends BlockchainMessage {
    private static final long serialVersionUID = 1L;

    protected int senderId;
    protected String signature;

    public ClientReq(int senderId, Long count) {
        super(BlockchainMessageType.CLIENT_REQ, count);
        this.senderId = senderId;
    }

    public abstract ClientReqType getReqType();

    @Override
    public int getSenderId() {
        return senderId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    // Enum for request types
    public enum ClientReqType {
        CONTRACT_CALL
    }
}