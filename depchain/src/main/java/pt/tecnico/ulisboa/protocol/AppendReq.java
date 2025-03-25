package pt.tecnico.ulisboa.protocol;

import pt.tecnico.ulisboa.utils.Logger;

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

            return super.equals(obj) && id.equals(other.id) &&
                    content.equals(other.content) && signature.equals(other.signature);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringShort();
    }

    public String toStringShort() {
        String str = "("  + content.toString() + ", "
                            + id + ", " 
                            + getCount() + ")";

        return str;
    }

    public String toStringExtended() {
        String str = "("  + content.toString() + ", "
                            + id + ", " 
                            + getCount() + ", "
                            + signature + ")";

        return str;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + super.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + content.hashCode();
        result = 31 * result + signature.hashCode();
        return result;
    }

}