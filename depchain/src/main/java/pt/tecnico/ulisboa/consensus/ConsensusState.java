package pt.tecnico.ulisboa.consensus;

import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

public class ConsensusState<T> {
    private T value;
    private int ts;
    private Map<T, Integer> writeSet =  new HashMap<>();

    public getState() {
        return Tuple(value, ts, writeSet);
    }
}
