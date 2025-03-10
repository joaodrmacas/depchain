package pt.tecnico.ulisboa.protocol;

public class ClientKeyRegister extends BlockchainMessage {
    
    private static final long serialVersionUID = 1L;

    private byte[] key;

    public ClientKeyRegister(byte[] key) {
        super(MessageType.CLIENT_KEY_REGISTER);
        this.key = key;
    }

    public byte[] getKey() {
        return key;
    }

}
