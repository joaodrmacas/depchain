package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.utils.RequiresEquals;

public class WriteMessage<T extends RequiresEquals> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;
    private T value;

    public WriteMessage(T value, int epochNumber) {
        super(MessageType.WRITE, epochNumber);

        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
