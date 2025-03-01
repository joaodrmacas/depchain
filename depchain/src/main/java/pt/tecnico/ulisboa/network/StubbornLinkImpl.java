package pt.tecnico.ulisboa.network;

import java.util.concurrent.*;
import java.util.*;
import java.net.*;

public class StubbornLinkImpl implements StubbornLink {
    private final FairLossLink fairLossLink;
    private final ConcurrentMap<String, DataMessage> pendingMessages = new ConcurrentHashMap<>();
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
        String destId = destination + ":" + port;
        long seqNum = nextSeqNum.getOrDefault(destId, 1L);
        nextSeqNum.put(destId, seqNum + 1);

        DataMessage msg = new DataMessage(destination, port, message, seqNum);
        msg.setKey(destId + ":" + seqNum);

        pendingMessages.putIfAbsent(msg.getId(), msg);
        fairLossLink.send(destination, port, msg.serialize());
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        fairLossLink.setMessageHandler((source, serializedMessage) -> {
            Message msg = Message.deserialize(serializedMessage);
            if (msg == null) return;
            
            if (msg.getType() == AckMessage.TYPE_INDICATOR) {
                // Handle ACK message
                //print received ack and who received it
                System.out.println("aqqqqqqqqqquiiiiiiiiiiiiiiiiiiiii");
                handleAck(source, msg.getSeqNum());
                return;
            } 
            else if (msg.getType() == DataMessage.TYPE_INDICATOR) { // TODO: nao percebo nada destas windows e acho que tamos a ignorar duplicate messages e tinhamos falado em reenviar os acks - Duarte
                // Handle data message
                DataMessage dataMsg = (DataMessage) msg;
                
                long seqNum = dataMsg.getSeqNum();
                String sourceId = source + ":" + dataMsg.getPort();
                
                // Initialize tracking structures if needed
                lastDelivered.putIfAbsent(sourceId, 0L);
                receivedSet.putIfAbsent(sourceId, ConcurrentHashMap.newKeySet());
                
                // Send ACK
                AckMessage ack = new AckMessage(source, dataMsg.getPort(), seqNum);
                fairLossLink.send(source, dataMsg.getPort(), ack.serialize());
                
                // Check if this is a duplicate or out of window
                long lastDeliveredSeq = lastDelivered.get(sourceId);
                
                // Duplicate, already delivered, ignore
                if (seqNum <= lastDeliveredSeq) {
                    return;
                }
                
                // Too far ahead, outside our window
                if (seqNum > lastDeliveredSeq + WINDOW_SIZE) {
                    return;
                }
                
                // Add to received set
                receivedSet.get(sourceId).add(seqNum);
                
                // Deliver the message to the handler
                handler.onMessage(source, dataMsg.getContent());
                
                // Update lastDelivered
                lastDelivered.put(sourceId, seqNum);
            }
            else {
                System.err.println("Received message with unknown type: " + msg.getType());
            }
        });
    }
    
    private void handleAck(String source, long ackSeqNum) {
        // Remove messages that have been ACKed
        for (Iterator<Map.Entry<String, DataMessage>> it = pendingMessages.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, DataMessage> entry = it.next();
            DataMessage msg = entry.getValue();
            // print source and destinatino
            System.out.println("Received ACK from " + source + " for message to " + msg.getDestination());
            if (msg.getDestination().equals(source) && msg.getSeqNum() <= ackSeqNum) {
                it.remove();
            }
        }
    }

    private void startRetransmissionLoop() {
        scheduler.scheduleAtFixedRate(() -> {
            for (DataMessage message : pendingMessages.values()) {
                if (message.getCounter() >= message.getCooldown()) {
                    message.setCounter(1);
                    fairLossLink.send(message.getDestination(), message.getPort(), message.serialize());
                    message.doubleCooldown();
                }
                else {
                    message.incrementCounter();
                }
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }
}