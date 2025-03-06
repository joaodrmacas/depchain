package pt.tecnico.ulisboa.consensus;

import pt.tecnico.ulisboa.network.AuthenticatedPerfectLink;

public class BFTConsensus<T> {
    private AuthenticatedPerfectLink link;
    private int processId;
    private Object txQueue;
    private ConsensusState<T> state = new ConsensusState<>();
    private int index;

    public BFTConsensus(int index, AuthenticatedPerfectLink link, int processId) {
        this.processId = processId;
        this.link = link;
        this.index = index;
    }

    public T start() throws UnsynchronizedNodeException {
        int epochNumber = 0;
        T value;
        
        while (true) {
            EpochConsensus<T> epoch = new EpochConsensus<>(index, link, processId, epochNumber, state);

            try {
                value = epoch.start();
            } catch (AbortedSignal abs) {
                System.out.println("Aborted: " + abs.getMessage());

                EpochChange epochChange = new EpochChange(index, link, processId, epochNumber);
                epochNumber = epochChange.start();

                continue;
            }
            break;
        }

        return value;
    }
}