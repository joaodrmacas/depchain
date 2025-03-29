package pt.tecnico.ulisboa.consensus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.server.Server;
import pt.tecnico.ulisboa.utils.types.Logger;
import pt.tecnico.ulisboa.utils.types.RequiresEquals;

public class BFTConsensus<T extends RequiresEquals> {
    private Server<T> member;
    private ExecutorCompletionService<Void> service;

    public BFTConsensus(Server<T> member) {
        this.member = member;
        this.service = new ExecutorCompletionService<>(member.getExecutor());
    }

    public void start() {
        AtomicInteger epochNumber = new AtomicInteger(0);
        int consensusIndex = 0;
        AtomicBoolean readPhaseDone = new AtomicBoolean(false);
        
        while (true) {
            T valueToBeProposed = null;

            AtomicInteger firstAwaken = new AtomicInteger(-1);

            // Create a list to keep track of all submitted futures
            List<Future<?>> waitingTasks = new ArrayList<>();

            clearTasksQueue();

            // Wait to be awaken either by new consensus from another member
            for (int i = 0; i < Config.NUM_MEMBERS; i++) {
                if (i == member.getId()) continue;

                final int _i = i, _consensusIndex = consensusIndex;
                Future<?> task = this.service.submit(() -> {
                    while (true) {
                        ConsensusMessage<T> msg = null;
                        try {
                            msg = member.peekConsensusMessageOrWait(_i, -1);
                        } catch (InterruptedException e) {
                            Logger.LOG("Interrupted while peeking consensus message");
                            return null;
                        }
                        catch (Exception e) {
                            Logger.ERROR("Error while peeking consensus message", e);
                        }

                        Logger.DEBUG("firstAwaken is " + firstAwaken.get());
                        if (firstAwaken.get() != -1) {
                            Logger.DEBUG("Work detected in another thread");
                            return null;
                        }
                        
                        if (msg != null) {
                            // Abort if received messaged from the future
                            if (isFromTheFuture(msg, epochNumber.get(), _consensusIndex)) {
                                Logger.DEBUG("Message from the future: "
                                    + "msg(e=" + msg.getEpochNumber() + ", c=" + msg.getConsensusIndex() + ")"
                                    + " | act(e=" + epochNumber.get() + ", c=" + _consensusIndex + ")");
                                return null;

                            } else if (isFromThePast(msg, epochNumber.get(), _consensusIndex)) {
                                Logger.DEBUG("Message from the past: "
                                    + "msg(e=" + msg.getEpochNumber() + ", c=" + msg.getConsensusIndex() + ")"
                                    + " | act(e=" + epochNumber.get() + ", c=" + _consensusIndex + ")");
                                member.removeFirstConsensusMessage(_i);
                                continue;
                            }

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
                    T tx = null;
                    try {
                        tx = member.peekReceivedTxOrWait(-1);
                    } catch (InterruptedException e) {
                        Logger.LOG("Interrupted while peeking received tx");
                        return null;
                    } catch (Exception e) {
                        Logger.ERROR("Error while peeking received tx", e);
                    }

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
                while (firstAwaken.get() == -1) {
                    this.service.take();
                }
                
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

            EpochConsensus<T> consensus = new EpochConsensus<>(member, epochNumber, consensusIndex, valueToBeProposed, readPhaseDone);

            Logger.DEBUG("Starting consensus " + consensusIndex + " for epoch " + epochNumber.get() + " with value " + valueToBeProposed);
            T value = consensus.start();

            member.pushDecidedTx(value);

            Logger.LOG("Consensus " + consensusIndex + " for epoch " + epochNumber.get() + " decided on value " + value);
        
            consensusIndex++;
        }
    }

    public boolean isFromTheFuture(ConsensusMessage<T> msg, int epochNumber, int consensusIndex) {
        return consensusIndex < msg.getConsensusIndex()
                || ( consensusIndex == msg.getConsensusIndex()) && epochNumber < msg.getEpochNumber();
    }

    public boolean isFromThePast(ConsensusMessage<T> msg, int epochNumber, int consensusIndex) {
        return consensusIndex > msg.getConsensusIndex()
                || ( consensusIndex == msg.getConsensusIndex()) && epochNumber > msg.getEpochNumber();
    }

    private void clearTasksQueue() {
        Future<Void> future;
        while ((future = this.service.poll()) != null) {
            future.cancel(true); // Cancels any incomplete tasks
        }
    }
}