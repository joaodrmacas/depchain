package pt.tecnico.ulisboa.consensus.message;

public class AcceptMessage<T> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;
    private T value;
    
    public AcceptMessage(T value) {
        super(MessageType.ACCEPT);

        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
