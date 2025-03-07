package pt.tecnico.ulisboa.consensus;

import pt.tecnico.ulisboa.network.AuthenticatedPerfectLink;



public class BFTConsensus<T> {
    private AuthenticatedPerfectLink link;
    private int processId;
    private Object txQueue;
    private ConsensusState<T> state = new ConsensusState<>();

    public BFTConsensus(AuthenticatedPerfectLink link, int processId) {
        this.processId = processId;
        this.link = link;
    }

    public T start() {
        int epochNumber = 0;
        T value;
        Boolean readPhaseDone = false;

        while (true) {
            EpochConsensus<T> epoch = new EpochConsensus<>(link, processId, epochNumber, state, readPhaseDone);

            try {
                value = epoch.start();
            } catch (AbortedSignal abs) {
                System.out.println("Aborted: " + abs.getMessage());

                EpochChange epochChange = new EpochChange(link, processId, epochNumber);
                epochNumber = epochChange.start();

                readPhaseDone = false;



                continue;
            }
            break;
        }

        return value;
    }
}