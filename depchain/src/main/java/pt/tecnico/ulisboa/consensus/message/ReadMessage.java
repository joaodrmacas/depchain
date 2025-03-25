package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.utils.RequiresEquals;

public class ReadMessage<T extends RequiresEquals> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;

    public ReadMessage(int epochNumber, int consensusIndex) {
        super(MessageType.READ, epochNumber, consensusIndex);
    }

    @Override
    public String toString() {
        return "READ(" + epochNumber + ')';
    }
}
