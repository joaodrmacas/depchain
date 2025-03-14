package pt.tecnico.ulisboa.consensus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.Node;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.utils.RequiresEquals;

public class BFTConsensus<T extends RequiresEquals> {
    private Node<T> member;

    public BFTConsensus(Node<T> member) {
        this.member = member;
    }

    public void start() {
        AtomicInteger epochNumber = new AtomicInteger(0);
        AtomicBoolean readPhaseDone = new AtomicBoolean(false);
        
        while (true) {
            T valueToBeProposed = null;

            AtomicInteger firstAwaken = new AtomicInteger(-1);

            // wait to be awaken either by new consensus from another member
            for (int i = 0; i < Config.NUM_MEMBERS; i++) {
                if (i == member.getId()) continue;

                final int _i = i;
                new Thread(() -> {
                    while (true) {
                        ConsensusMessage<T> msg = member.peekConsensusMessageOrWait(_i);

                        if (firstAwaken.get() != -1) return;
                        
                        if (msg != null) {
                            firstAwaken.set(_i);
                            return;
                        }
                    }
                }).start();
            }
            
            // or wait to be awaken by new tx from clients
            while (true) {
                T msg = member.peekReceivedTxOrWait();

                if (firstAwaken.get() != -1) return;

                if (msg != null) {
                    valueToBeProposed = msg;
                    firstAwaken.set(Config.NUM_MEMBERS);
                    break;
                }
            }
            
            // Try one last time to peek a value (new tx from clients)
            if (valueToBeProposed == null) {
                valueToBeProposed = member.peekReceivedTx();
            }

            EpochConsensus<T> epoch = new EpochConsensus<>(member, epochNumber, valueToBeProposed, readPhaseDone);

            T value = epoch.start();

            member.removeReceivedTx(value);

            member.pushDecidedTx(value);
        }
    }
}