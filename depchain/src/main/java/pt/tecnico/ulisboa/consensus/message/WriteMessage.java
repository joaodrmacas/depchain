package pt.tecnico.ulisboa.consensus.message;

import pt.tecnico.ulisboa.consensus.WriteTuple;

public class WriteMessage<T> extends ConsensusMessage<T> {
    private static final long serialVersionUID = 1L;
    private WriteTuple<T> tuple;

    public WriteMessage(WriteTuple<T> tuple) {
        super(MessageType.WRITE);

        this.tuple = tuple;
    }

    public WriteTuple<T> getTuple() {
        return tuple;
    }
}
