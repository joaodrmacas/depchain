package pt.tecnico.ulisboa.consensus;

import java.util.HashMap;
import java.util.Map;

import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.Logger;

public class CollectedStates<T> {
    private Map<Integer, ConsensusState<T>> states = new HashMap<>();

    public CollectedStates(int memberCount) {
        for (int i = 0; i < memberCount; i++) {
            this.states.put(i, null);
        }
    }

    public Map<Integer, ConsensusState<T>> getStates() {
        return this.states;
    }

    public void addState(int memberId, ConsensusState<T> state) {
        if (this.states.containsKey(memberId)) {
            this.states.put(memberId, state);
        } else {
            Logger.ERROR("member ID not found in collected states");
        }
    }
}
