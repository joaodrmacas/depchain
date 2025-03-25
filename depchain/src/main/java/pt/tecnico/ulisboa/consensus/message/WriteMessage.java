package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.utils.types.RequiresEquals;

public class WriteMessage<T extends RequiresEquals> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;
    private T value;

    public WriteMessage(T value, int epochNumber, int consensusIndex) {
        super(MessageType.WRITE, epochNumber, consensusIndex);

        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "WRITE(" + "v=" + value + ", e=" + epochNumber + ")";
    }
}
