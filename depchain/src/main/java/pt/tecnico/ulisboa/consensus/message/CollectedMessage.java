package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.consensus.CollectedStates;

public class CollectedMessage extends ConsensusMessage {
    private static final long serialVersionUID = 1L;
    CollectedStates states;

    public CollectedMessage(CollectedStates states, int epochNumber, int consensusIndex) {
        super(MessageType.COLLECTED, epochNumber, consensusIndex);

        this.states = states;
    }
    
    public CollectedStates getStates() {
        return states;
    }

    @Override
    public String toString() {
        return "COLLECTED(e=" + epochNumber + "\nstates=" + states + ")";
    }
}
