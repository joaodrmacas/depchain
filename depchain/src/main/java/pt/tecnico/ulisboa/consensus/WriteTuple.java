package pt.tecnico.ulisboa.consensus;

import java.io.Serializable;

import pt.tecnico.ulisboa.utils.types.Consensable;

public class WriteTuple implements Serializable {
    private static final long serialVersionUID = 1L;

    private Consensable value;
    private int ts;

    public WriteTuple(Consensable value, int ts) {
        this.value = value;
        this.ts = ts;
    }

    public Consensable getValue() {
        return value;
    }

    public int getTimestamp() {
        return ts;
    }

    @Override
    public String toString() {
        return "[val=" + value + ", ts=" + ts + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj.getClass() != this.getClass()) return false;

        WriteTuple other = (WriteTuple) obj;
        return value.equals(other.value) && ts == other.ts;
    }

    public boolean isValid() {
        return ts >= 0;
    }
}
