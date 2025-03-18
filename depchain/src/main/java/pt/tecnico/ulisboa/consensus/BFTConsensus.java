package pt.tecnico.ulisboa.consensus;

import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.Node;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.RequiresEquals;

public class BFTConsensus<T extends RequiresEquals> {
    private Node<T> member;
    private ExecutorCompletionService<Void> service;

    public BFTConsensus(Node<T> member) {
        this.member = member;
        this.service = new ExecutorCompletionService<>(member.getExecutor());
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
                this.service.submit(() -> {
                    while (true) {
                        ConsensusMessage<T> msg = member.peekConsensusMessageOrWait(_i, -1);

                        if (firstAwaken.get() != -1) {
                            Logger.DEBUG("Work detected in another thread");
                            return null;
                        }
                        
                        if (msg != null) {
                            Logger.DEBUG("Consensus work detected");
                            firstAwaken.set(_i);
                            return null;
                        }
                    }
                });
            }

            // or wait to be awaken by new tx from clients
            this.service.submit(() -> {
                while (true) {
                    T tx = member.peekReceivedTxOrWait(Config.NEW_TRANSACTION_TIMEOUT);

                    if (firstAwaken.get() != -1) {
                        Logger.DEBUG("Work detected in another thread");
                        return null;
                    }

                    if (tx != null) {
                        Logger.DEBUG("New transaction detected");
                        firstAwaken.set(Config.NUM_MEMBERS);
                        return null;
                    }
                }
            });

            try {
                Logger.DEBUG("Waiting for work");
                this.service.take();
            } catch (Exception e) {
                Logger.ERROR("Error while waiting for first awaken", e);
            }

            // Try one last time to peek a value (new tx from clients)
            if (valueToBeProposed == null) {
                valueToBeProposed = member.peekReceivedTx();
            }

            EpochConsensus<T> consensus = new EpochConsensus<>(member, epochNumber, valueToBeProposed, readPhaseDone);

            Logger.DEBUG("Starting consensus for epoch " + epochNumber.get() + " with value " + valueToBeProposed);
            T value = consensus.start();

            member.removeReceivedTx(value);

            member.pushDecidedTx(value);

            Logger.LOG("Consensus for epoch " + epochNumber.get() + " decided on value " + value);
        }
    }
}