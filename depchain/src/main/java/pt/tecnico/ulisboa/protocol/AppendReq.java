package pt.tecnico.ulisboa.protocol;

// TODO: This should be deleted. It is here for compatibility with previous versions -> Duarte
public class AppendReq<T> extends ClientReq {
    private T content;

    public AppendReq(Integer id, String signature, long count, T content) {
        super(id, signature, count);
        this.content = content;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.TRANSFER; // Default to maintain previous behavior
    }

    public T getMessage() {
        return content;
    }

    @Override
    public String toString() {
        return String.format("AppendReq(id=%d, content=%s)", id, content);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        
        AppendReq<?> appendReq = (AppendReq<?>) o;
        return content.equals(appendReq.content);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + content.hashCode();
        return result;
    }
}