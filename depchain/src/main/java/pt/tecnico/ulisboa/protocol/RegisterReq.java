package pt.tecnico.ulisboa.protocol;

public class RegisterReq extends BlockchainMessage {
    
    private static final long serialVersionUID = 1L;

    private byte[] key;

    public RegisterReq(byte[] key, long seqNum) {
        super(BlockchainMessageType.REGISTER_REQ, seqNum);
        this.key = key;
    }

    public byte[] getKey() {
        return key;
    }

}
