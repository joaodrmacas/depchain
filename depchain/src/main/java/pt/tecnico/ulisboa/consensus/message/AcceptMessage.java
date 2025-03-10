package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.utils.RequiresEquals;

public class AcceptMessage<T extends RequiresEquals> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;
    private T value;
    
    public AcceptMessage(T value, int epochNumber) {
        super(MessageType.ACCEPT, epochNumber);

        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
