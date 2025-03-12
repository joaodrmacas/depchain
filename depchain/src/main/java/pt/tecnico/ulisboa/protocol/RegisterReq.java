package pt.tecnico.ulisboa.protocol;

public class RegisterReq extends BlockchainMessage {
    
    private static final long serialVersionUID = 1L;

    private byte[] key;
    private int senderId;

    public RegisterReq(int senderId, byte[] key, long seqNum) {
        super(BlockchainMessageType.REGISTER_REQ, seqNum);
        this.key = key;
        this.senderId = senderId;
    }

    public byte[] getKey() {
        return key;
    }

    public int getSenderId() {
        return senderId;
    }

}
