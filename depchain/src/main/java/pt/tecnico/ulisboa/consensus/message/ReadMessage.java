package pt.tecnico.ulisboa.consensus.message;

public class ReadMessage<T> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;

    public ReadMessage() {
        super(MessageType.READ);
    }
}
