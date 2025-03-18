package pt.tecnico.ulisboa.consensus;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.RequiresEquals;

public class CollectedStates<T extends RequiresEquals> implements Serializable {
    private static final long serialVersionUID = 1L;
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
            Logger.ERROR("member ID not found in collected states", new Exception());
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
                Logger.LOG("state " + i + " not found in collected states");
                return false;
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

    public String toString() {
        String res = "{\n";
        for (Map.Entry<Integer, ConsensusState<T>> entry : this.states.entrySet()) {
            res += "\t" + entry.getKey() + ": " + entry.getValue() + "\n";
        }
        return res + "}\n";
    }
}
