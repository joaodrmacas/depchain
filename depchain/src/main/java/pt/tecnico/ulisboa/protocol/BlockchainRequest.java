package pt.tecnico.ulisboa.protocol;

public class BlockchainRequest<T> extends BlockchainMessage {
    private static final long serialVersionUID = 1L;
    
    private Integer id;
    private T content;
    private long seqNum;
    private String signature;

    public BlockchainRequest(Integer id, T content, long seqNum, String signature) {
        super(MessageType.BLOCKCHAIN_REQ);
        this.id = id;
        this.content = content;
        this.seqNum = seqNum;
        this.signature = signature;
    }

    public T getMessage() {
        return content;
    }

    public Integer getId() {
        return id;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockchainRequest) {
            BlockchainRequest<?> other = (BlockchainRequest<?>) obj;
            return id.equals(other.id) && content.equals(other.content) && seqNum == other.seqNum && signature.equals(other.signature);
        }
        return false;
    }

}