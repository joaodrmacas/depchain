package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.consensus.CollectedStates;
import pt.tecnico.ulisboa.utils.RequiresEquals;

public class CollectedMessage<T extends RequiresEquals> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;
    CollectedStates<T> states;

    public CollectedMessage(CollectedStates<T> states, int epochNumber) {
        super(MessageType.COLLECTED, epochNumber);

        this.states = states;
    }
    
    public CollectedStates<T> getStates() {
        return states;
    }
}
