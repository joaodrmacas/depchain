package pt.tecnico.ulisboa.consensus;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import pt.tecnico.ulisboa.utils.types.Logger;

public class CollectedStates implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<Integer, ConsensusState> states = new HashMap<>();
    private int memberCount;

    public CollectedStates(int memberCount) {
        this.memberCount = memberCount;
        for (int i = 0; i < this.memberCount; i++) {
            this.states.put(i, null);
        }
    }

    public Map<Integer, ConsensusState> getStates() {
        return this.states;
    }

    public void addState(int memberId, ConsensusState state) {
        if (this.states.containsKey(memberId)) {
            this.states.put(memberId, state);
        } else {
            Logger.ERROR("member ID not found in collected states", new Exception());
        }
    }

    public boolean verifyStates(Map<Integer, PublicKey> publicKeys) {
        for (int i = 0; i < this.memberCount; i++) {
            ConsensusState state = this.states.get(i);
            if (state != null) {
                if (!state.isValid(publicKeys.get(i))) {
                    Logger.LOG("state " + i + " is not valid");
                    return false;
                }
            } else {
                Logger.DEBUG("state " + i + " not found in collected states");
            }
        }
        return true;
    }

    public void overwriteWith(CollectedStates other) {
        this.states.clear();
        for (Map.Entry<Integer, ConsensusState> entry : other.states.entrySet()) {
            this.states.put(entry.getKey(), entry.getValue());   
        }
    }

    public String toString() {
        String res = "{\n";
        for (Map.Entry<Integer, ConsensusState> entry : this.states.entrySet()) {
            res += "\t" + entry.getKey() + ": " + entry.getValue() + "\n";
        }
        return res + "}\n";
    }
}
