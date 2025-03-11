package pt.tecnico.ulisboa.consensus;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.RequiresEquals;

public class CollectedStates<T extends RequiresEquals> {
    private Map<Integer, ConsensusState<T>> states = new HashMap<>();
    private int memberCount;

    public CollectedStates(int memberCount) {
        this.memberCount = memberCount;
        for (int i = 0; i < this.memberCount; i++) {
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

    public boolean verifyStates(Map<Integer, PublicKey> publicKeys) {
        for (int i = 0; i < this.memberCount; i++) {
            ConsensusState<T> state = this.states.get(i);
            if (state != null) {
                if (!state.isValid(publicKeys.get(i))) {
                    return false;
                }
            } else {
                Logger.ERROR("state not found in collected states");
            }
        }
        return true;
    }

    public void overwriteWith(CollectedStates<T> other) {
        this.states.clear();
        for (Map.Entry<Integer, ConsensusState<T>> entry : other.states.entrySet()) {
            this.states.put(entry.getKey(), entry.getValue());   
        }
    }
}
