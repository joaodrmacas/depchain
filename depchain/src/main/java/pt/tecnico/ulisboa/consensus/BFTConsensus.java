package pt.tecnico.ulisboa.consensus;

import pt.tecnico.ulisboa.Node;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.RequiresEquals;

public class BFTConsensus<T extends RequiresEquals> {
    private Node<T> member;

    public BFTConsensus(Node<T> member) {
        this.member = member;
    }

    public void start() {
        int epochNumber = 0;
        Boolean readPhaseDone = false;
        
        while (true) {
            T value;
            T valueToBeProposed = member.fetchReceivedTx();

            if (valueToBeProposed == null) {
                // wait to be awaken either by new tx or by new consensus
                // from another member
                // if (awakened by new tx) continue;
            }

            ConsensusState<T> state = 
                new ConsensusState<>(new WriteTuple<>(valueToBeProposed, 0));

            while (true) {
                EpochConsensus<T> epoch = new EpochConsensus<>(member, epochNumber, state, readPhaseDone);

                try {
                    value = epoch.start(valueToBeProposed);
                } catch (AbortedSignal abs) {
                    Logger.LOG("Aborted: " + abs.getMessage());

                    EpochChange<T> epochChange = new EpochChange<>(member, epochNumber);
                    epochNumber = epochChange.start();

                    readPhaseDone = false;

                    continue;
                }
                break;
            }

            member.pushDecidedTx(value);
        }
    }
}
//mas haver uma geral que ´e ecapsulasse as duas?
//