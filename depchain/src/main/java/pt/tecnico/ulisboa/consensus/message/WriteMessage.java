package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.utils.types.Consensable;

public class WriteMessage extends ConsensusMessage {
    private static final long serialVersionUID = 1L;
    private Consensable value;

    public WriteMessage(Consensable value, int epochNumber, int consensusIndex) {
        super(MessageType.WRITE, epochNumber, consensusIndex);

        this.value = value;
    }

    public Consensable getValue() {
        return value;
    }

    public void setValue(Consensable value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "WRITE(" + "v=" + value + ", e=" + epochNumber + ")";
    }
}
