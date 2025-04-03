package pt.tecnico.ulisboa.consensus.message;

public class NewEpochMessage extends ConsensusMessage {
    private static final long serialVersionUID = 1L;

    public NewEpochMessage(int epochNumber, int consensusIndex) {
        super(MessageType.NEWEPOCH, epochNumber, consensusIndex);
    }

    @Override
    public String toString() {
        return "NEWEPOCH(" + getEpochNumber() + ')';
    }
}
