package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.utils.types.RequiresEquals;

public class AcceptMessage<T extends RequiresEquals> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;
    private T value;
    
    public AcceptMessage(T value, int epochNumber, int consensusIndex) {
        super(MessageType.ACCEPT, epochNumber, consensusIndex);

        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ACCEPT(" + "v=" + value + ", e=" + epochNumber + ")";
    }
}
