package pt.tecnico.ulisboa.consensus.message;

import java.util.List;

import pt.tecnico.ulisboa.consensus.ConsensusState;

public class CollectedMessage<T> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;
    List<ConsensusState<T>> states;

    public CollectedMessage(List<ConsensusState<T>> states) {
        super(MessageType.COLLECTED);

        this.states = states;
    }
    
    public List<ConsensusState<T>> getStates() {
        return states;
    }
}
