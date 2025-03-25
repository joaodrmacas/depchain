package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.consensus.ConsensusState;
import pt.tecnico.ulisboa.utils.RequiresEquals;

public class StateMessage<T extends RequiresEquals> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;
    private ConsensusState<T> state;

    public StateMessage(ConsensusState<T> state, int epochNumber, int consensusIndex) {
        super(MessageType.STATE, epochNumber, consensusIndex);

        this.state = state;
    }

    public ConsensusState<T> getState() {
        return state;
    }

    @Override
    public String toString() {
        return "STATE(" + "s=" + state + ", e=" + epochNumber + ")";
    }
}
