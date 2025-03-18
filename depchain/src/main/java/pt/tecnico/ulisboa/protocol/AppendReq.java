package pt.tecnico.ulisboa.protocol;

public class AppendReq<T> extends BlockchainMessage {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private T content;
    private String signature;

    public AppendReq(Integer id, T content, long count, String signature) {
        super(BlockchainMessageType.APPEND_REQ, count);
        this.id = id;
        this.content = content;
        this.signature = signature;
    }

    public T getMessage() {
        return content;
    }

    public Integer getId() {
        return id;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AppendReq) {
            AppendReq<?> other = (AppendReq<?>) obj;
            return getType() == other.getType() && id.equals(other.id) &&
                    content.equals(other.content) && getCount() == other.getCount()
                    && signature.equals(other.signature);
        }
        return false;
    }

    @Override
    public String toString() {
        String str = "("  + content.toString() + ", "
                            + id + ", " 
                            + getCount() + ")";

        return str;
    }

    public String toStringShort() {
        return "R" + id.toString();
    }

}