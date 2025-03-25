package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.utils.RequiresEquals;

public class NewEpochMessage<T extends RequiresEquals> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;

    public NewEpochMessage(int epochNumber, int consensusIndex) {
        super(MessageType.NEWEPOCH, epochNumber, consensusIndex);
    }

    @Override
    public String toString() {
        return "NEWEPOCH(" + getEpochNumber() + ')';
    }
}
