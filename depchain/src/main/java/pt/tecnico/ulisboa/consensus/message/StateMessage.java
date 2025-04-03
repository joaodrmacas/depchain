package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.consensus.ConsensusState;

public class StateMessage extends ConsensusMessage {
    private static final long serialVersionUID = 1L;
    private ConsensusState state;

    public StateMessage(ConsensusState state, int epochNumber, int consensusIndex) {
        super(MessageType.STATE, epochNumber, consensusIndex);

        this.state = state;
    }

    public ConsensusState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "STATE(" + "s=" + state + ", e=" + epochNumber + ")";
    }
}
