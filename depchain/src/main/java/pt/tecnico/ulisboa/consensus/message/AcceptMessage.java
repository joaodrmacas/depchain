package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.utils.types.Consensable;

public class AcceptMessage extends ConsensusMessage {
    private static final long serialVersionUID = 1L;
    private Consensable value;
    
    public AcceptMessage(Consensable value, int epochNumber, int consensusIndex) {
        super(MessageType.ACCEPT, epochNumber, consensusIndex);

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
        return "ACCEPT(" + "v=" + value + ", e=" + epochNumber + ")";
    }
}
