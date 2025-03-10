package pt.tecnico.ulisboa.protocol;

public class KeyRegisterReq extends BlockchainMessage {
    
    private static final long serialVersionUID = 1L;

    private byte[] key;

    public KeyRegisterReq(byte[] key, long seqNum) {
        super(BlockchainMessageType.KEY_REGISTER_REQ, seqNum);
        this.key = key;
    }

    public byte[] getKey() {
        return key;
    }

}
