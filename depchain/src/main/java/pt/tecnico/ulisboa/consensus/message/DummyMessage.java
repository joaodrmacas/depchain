package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.utils.RequiresEquals;

// only used for testing purposes
public class DummyMessage<T extends RequiresEquals> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;

    private Object dummy;

    public DummyMessage(int epochNumber, Object dummy) {
        super(MessageType.DUMMY, epochNumber);
        this.dummy = dummy;
    }

    @Override
    public String toString() {
        return "DUMMY(" + dummy + ", " + epochNumber + ')';
    }
}
