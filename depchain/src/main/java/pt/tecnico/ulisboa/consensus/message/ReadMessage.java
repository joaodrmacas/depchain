package pt.tecnico.ulisboa.consensus.message;

public class ReadMessage extends ConsensusMessage {
    private static final long serialVersionUID = 1L;

    public ReadMessage(int epochNumber, int consensusIndex) {
        super(MessageType.READ, epochNumber, consensusIndex);
    }

    @Override
    public String toString() {
        return "READ(" + epochNumber + ')';
    }
}
