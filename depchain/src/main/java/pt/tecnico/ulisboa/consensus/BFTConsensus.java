package pt.tecnico.ulisboa.consensus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
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

            // Create a list to keep track of all submitted futures
            List<Future<?>> waitingTasks = new ArrayList<>();

            // Wait to be awaken either by new consensus from another member
            for (int i = 0; i < Config.NUM_MEMBERS; i++) {
                if (i == member.getId()) continue;

                final int _i = i;
                Future<?> task = this.service.submit(() -> {
                    while (true) {
                        ConsensusMessage<T> msg = member.peekConsensusMessageOrWait(_i, -1);

                        Logger.DEBUG("firstAwaken is " + firstAwaken.get());
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
                
                waitingTasks.add(task);
            }

            // Wait to be awaken by new tx from clients
            Future<?> txTask = this.service.submit(() -> {
                while (true) {
                    T tx = member.peekReceivedTxOrWait(-1);

                    if (firstAwaken.get() != -1) {
                        Logger.DEBUG("Work detected in another thread");
                        return null;
                    }

                    if (tx != null) {
                        Logger.DEBUG("New transaction detected");
                        firstAwaken.set(Config.NUM_MEMBERS);
                        Logger.DEBUG("firstAwaken set to " + firstAwaken.get());
                        return null;
                    }
                }
            });

            waitingTasks.add(txTask);

            try {
                Logger.DEBUG("Waiting for work");
                // Wait for any of the tasks to complete
                this.service.take();
                
                // Ensure all tasks are cancelled after one completes
                for (Future<?> task : waitingTasks) {
                    if (!task.isDone()) {
                        task.cancel(true);
                    }
                }
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

            member.pushDecidedTx(value);

            Logger.LOG("Consensus for epoch " + epochNumber.get() + " decided on value " + value);
        }
    }
}