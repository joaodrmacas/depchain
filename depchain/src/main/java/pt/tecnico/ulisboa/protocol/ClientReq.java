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
        CONTRACT_CALL,
        TRANSFER_DEP_COIN,
        BALANCE_OF_DEP_COIN
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ClientReq)) {
            return false;
        }
        ClientReq other = (ClientReq) obj;
        return super.equals(obj) && senderId == other.senderId;
    }

    public abstract boolean needsConsensus();
}