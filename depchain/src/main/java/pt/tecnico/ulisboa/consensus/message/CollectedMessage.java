package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.consensus.CollectedStates;
import pt.tecnico.ulisboa.utils.types.RequiresEquals;

public class CollectedMessage<T extends RequiresEquals> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;
    CollectedStates<T> states;

    public CollectedMessage(CollectedStates<T> states, int epochNumber, int consensusIndex) {
        super(MessageType.COLLECTED, epochNumber, consensusIndex);

        this.states = states;
    }
    
    public CollectedStates<T> getStates() {
        return states;
    }

    @Override
    public String toString() {
        return "COLLECTED(e=" + epochNumber + "\nstates=" + states + ")";
    }
}
