package pt.tecnico.ulisboa.consensus;

import java.io.Serializable;

public class WriteTuple<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private T value;
    private int ts;

    public WriteTuple(T value, int ts) {
        this.value = value;
        this.ts = ts;
    }

    public T getValue() {
        return value;
    }

    public int getTimestamp() {
        return ts;
    }

    @Override
    public String toString() {
        return "[val=" + value + ", ts=" + ts + "]";
    }
}
