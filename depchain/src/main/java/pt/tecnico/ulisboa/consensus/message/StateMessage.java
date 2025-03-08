package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.consensus.ConsensusState;

public class StateMessage<T> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;
    private ConsensusState<T> state;

    public StateMessage(ConsensusState<T> state) {
        super(MessageType.STATE);

        this.state = state;
    }

    public ConsensusState<T> getState() {
        return state;
    }
}
