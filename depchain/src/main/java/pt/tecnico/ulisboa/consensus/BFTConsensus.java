package pt.tecnico.ulisboa.consensus;

import pt.tecnico.ulisboa.network.AuthenticatedPerfectLink;
import pt.tecnico.ulisboa.utils.Logger;

public class BFTConsensus<T> {
    private AuthenticatedPerfectLink link;
    private int memberId;

    public BFTConsensus(AuthenticatedPerfectLink link, int memberId) {
        this.memberId = memberId;
        this.link = link;
    }

    public T start(T valueToBeProposed) {
        int epochNumber = 0;
        Boolean readPhaseDone = false;
        T value;
        ConsensusState<T> state = new ConsensusState<>();

        while (true) {
            EpochConsensus<T> epoch = new EpochConsensus<>(link, memberId, epochNumber, state, readPhaseDone);

            try {
                value = epoch.start(valueToBeProposed);
            } catch (AbortedSignal abs) {
                Logger.LOG("Aborted: " + abs.getMessage());

                EpochChange epochChange = new EpochChange(link, memberId, epochNumber);
                epochNumber = epochChange.start();

                readPhaseDone = false;

                continue;
            }
            break;
        }

        return value;
    }
}