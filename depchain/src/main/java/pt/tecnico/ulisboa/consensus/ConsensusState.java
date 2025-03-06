package pt.tecnico.ulisboa.consensus;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ConsensusState<T> implements Serializable {
    private static final long serialVersionUID = 1L; // Ensures serialization compatibility

    private WriteTuple<T> mostRecentQuorumWritten;
    private Map<T, WriteTuple<T>> writeSet = new HashMap<>();

    public WriteTuple<T> getMostRecentQuorumWritten() {
        return mostRecentQuorumWritten;
    }

    public void setMostRecentQuorumWritten(WriteTuple<T> mostRecentQuorumWritten) {
        this.mostRecentQuorumWritten = mostRecentQuorumWritten;
    }

    public Map<T, WriteTuple<T>> getWriteSet() {
        return writeSet;
    }
}
