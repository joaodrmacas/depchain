package pt.tecnico.ulisboa.network;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.*;
import java.util.*;
import java.net.*;

public class StubbornLinkImpl implements StubbornLink {
    private final FairLossLink fairLossLink;
    private final ConcurrentMap<String, Message> pendingMessages = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final ConcurrentMap<String, Long> nextSeqNum = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> lastDelivered = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Long>> receivedSet = new ConcurrentHashMap<>();

    private static final int WINDOW_SIZE = 1000; //TODO: what val?
    
    public StubbornLinkImpl(int port) throws SocketException {
        this.fairLossLink = new UdpFairLossLink(port);
        startRetransmissionLoop();
    }

    @Override
    public void send(String destination, int port, byte[] message) {
        //make the keyy a timestamp
        String destId = destination + ":" + port;
        long seqNum = nextSeqNum.getOrDefault(destId, 1L);
        nextSeqNum.put(destId, seqNum + 1);

        Message msg = new Message(destination, port, message, seqNum);

        pendingMessages.putIfAbsent(msg.getId(), msg);
        fairLossLink.send(destination, port, message);
    }

    private void sendAck(String destination, int port, long seqNum, long msgSize) {
        // send ack as an object of msg class
        //instead of empty content send ack in content
        Message ack = new Message(destination, port, "ack", seqNum+msgSize);
        fairLossLink.send(destination, port, ack.serialize());
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        fairLossLink.setMessageHandler((source, serializedMessage) -> {
            Message msg = Message.deserialize(serializedMessage);
            if (msg == null) return;
            
            long seqNum = msg.getSeqNum();
            String sourceId = source + ":" + msg.port;
            
            // Initialize tracking structures if needed
            lastDelivered.putIfAbsent(sourceId, 0L);
            receivedSet.putIfAbsent(sourceId, ConcurrentHashMap.newKeySet());
            
            // Always send ACK
            //TODO: implement
            this.sendAck(msg.getDestination(), msg.getPort(), seqNum, msg.getContent().length);
            
            // Check if this is a duplicate or out of window
            long lastDeliveredSeq = lastDelivered.get(sourceId);
            
            // Duplicate, already delivered, ignore
            if (seqNum <= lastDeliveredSeq) {
                return;
            }
            
            // Too far ahead, outside our window
            // You could buffer this or implement a recovery mechanism
            if (seqNum > lastDeliveredSeq + WINDOW_SIZE) {
                return;
            }
            
            // Add to received set
            receivedSet.get(sourceId).add(seqNum);
            
            // Try to deliver in order
            // deliverInOrder(sourceId, handler);
        });
    }

    private void startRetransmissionLoop() {
        scheduler.scheduleAtFixedRate(() -> {
            for (Message message : pendingMessages.values()) {
                if(message.getCounter() >= message.getCooldown()) { // TODO should be == mas assim nao da fs
                    message.setCounter(1);
                    fairLossLink.send(message.getDestination(), message.getPort(), message.getContent()); // Retransmit message
                    message.doubleCooldown();
                }
                else {
                    message.incrementCounter();
                }
        }
        }, 500, 500, TimeUnit.MILLISECONDS); // Fixed loop, checks every 500ms
    }

}
