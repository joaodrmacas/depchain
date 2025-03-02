package pt.tecnico.ulisboa.network.layers;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.network.message.*;
import java.util.concurrent.*;
// import java.util.*;
import java.net.*;

public class StubbornLinkImpl implements StubbornLink {
    private final FairLossLink fairLossLink;
    private final ConcurrentMap<String, DataMessage> pendingMessages = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final ConcurrentMap<String, Long> nextSeqNum = new ConcurrentHashMap<>();
    // private final ConcurrentMap<String, Long> lastDelivered = new ConcurrentHashMap<>();
    // private final ConcurrentMap<String, Set<Long>> receivedSet = new ConcurrentHashMap<>();

    // private static final int WINDOW_SIZE = 1000; // TODO: what val?

    public StubbornLinkImpl(String destIP, int destPort) throws SocketException {
        this.fairLossLink = new FairLossLinkImpl(destIP, destPort);
        startRetransmissionLoop();
    }

    @Override
    public void send(String destIP, int destPort, byte[] message) {
        String destId = destIP + ":" + destPort;
        long seqNum = nextSeqNum.getOrDefault(destId, 1L);
        nextSeqNum.put(destId, seqNum + 1);

        DataMessage msg = new DataMessage(destIP, destPort, message, seqNum);
        msg.setKey(destId + ":" + seqNum);

        pendingMessages.putIfAbsent(msg.getId(), msg);
        fairLossLink.send(destIP, destPort, msg.serialize());
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        fairLossLink.setMessageHandler((source, serializedMessage) -> {
            Message msg = Message.deserialize(serializedMessage);
            if (msg == null) return;
            
            if (msg.getType() == AckMessage.TYPE_INDICATOR) {
                handleAckMessage(source, msg.getSeqNum()); 
                return;
            } 
            else if (msg.getType() == DataMessage.TYPE_INDICATOR) {
                handleNormalMessage(source, msg, handler);
            }
            else {
                System.err.println("Received message with unknown type: " + msg.getType());
            }
        });
    }

    private void handleNormalMessage(String source, Message msg, MessageHandler handler) {
        DataMessage dataMsg = (DataMessage) msg;

        long seqNum = msg.getSeqNum();
        
        // Initialize tracking structures if needed
        // lastDelivered.putIfAbsent(source, 0L);
        // receivedSet.putIfAbsent(source, ConcurrentHashMap.newKeySet());
        
        // Send ACK
        String[] sourceParts = source.split(":");
        String sourceIP = sourceParts[0];
        int sourcePort = Integer.parseInt(sourceParts[1]);

        AckMessage ack = new AckMessage(seqNum);
        fairLossLink.send(sourceIP, sourcePort, ack.serialize());
        
        // Check if this is a duplicate dataMsg or out of window
        // long lastDeliveredSeq = lastDelivered.get(source);
        
        // Duplicate, already delivered, ignore
        /*
        if (seqNum <= lastDeliveredSeq) {
            return;
        }
        
        // Too far ahead, outside our window
        if (seqNum > lastDeliveredSeq + WINDOW_SIZE) {
            return;
        }
        
        // Add to received set
        receivedSet.get(source).add(seqNum);
        */
        
        // Deliver the message to the handler
        handler.onMessage(source, dataMsg.getContent());
        
        // Update lastDelivered
        /*
        lastDelivered.put(source, seqNum);
        */
    }

    private void handleAckMessage(String source, long ackSeqNum) {
        String sourceId = source + ":" + ackSeqNum;
        DataMessage msg = pendingMessages.remove(sourceId);
        if (msg == null) {
            System.err.println("Received ACK for unknown message: " + sourceId);
            return;
        }
    }

    private void startRetransmissionLoop() {
        scheduler.scheduleAtFixedRate(() -> {
            for (DataMessage message : pendingMessages.values()) {
                if (message.getCounter() >= message.getCooldown()) {
                    message.setCounter(1);
                    fairLossLink.send(message.getDestination(), message.getPort(), message.serialize());
                    message.doubleCooldown();
                } else {
                    message.incrementCounter();
                }
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }
}