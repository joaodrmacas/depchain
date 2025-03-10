package pt.tecnico.ulisboa.protocol;

public class BlockchainRequest<T> extends BlockchainMessage<T> {
    private static final long serialVersionUID = 1L;
    
    private Integer id;
    private T content;

    public BlockchainRequest(Integer id, T content) {
        super(MessageType.CLIENT_REQUEST);
        this.id = id;
        this.content = content;
    }

    public T getMessage() {
        return content;
    }

    public Integer getId() {
        return id;
    }

    public void setContent(T content){
        this.content = content;
    }

    public void setClientId(Integer id){
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockchainRequest) {
            BlockchainRequest<?> other = (BlockchainRequest<?>) obj;
            return id.equals(other.id) && content.equals(other.content);
        }
        return false;
    }

}