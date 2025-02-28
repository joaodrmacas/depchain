package pt.tecnico.ulisboa.network;

import java.util.concurrent.*;
import java.net.*;


public class StubbornLinkImpl implements StubbornLink {
    private final FairLossLink fairLossLink;
    private final ConcurrentMap<String, Message> pendingMessages = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public StubbornLinkImpl(int port) throws SocketException {
        this.fairLossLink = new UdpFairLossLink(port);
        startRetransmissionLoop();
    }

    @Override
    public void send(String destination, int port, byte[] message) {
        //make the keyy a timestamp
        Message msg = new Message(destination, port, message);
        msg.setKey(String.valueOf(System.currentTimeMillis()));
        pendingMessages.putIfAbsent(msg.getKey(), msg);
        fairLossLink.send(destination, port, message); // Initial send
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        fairLossLink.setMessageHandler((source, message) -> {
            Message msg = Message.deserialize(message);
            String key = msg.getKey();
            pendingMessages.remove(key); // Stop retransmitting
            handler.onMessage(source, message);
        });
    }

    private void startRetransmissionLoop() {
        scheduler.scheduleAtFixedRate(() -> {
            for (Message message : pendingMessages.values()) {
                if(message.getCounter() >= message.getCooldown()) { // should be == mas assim nao da merda fs
                    message.setCounter(1);
                    fairLossLink.send(message.destination, message.port, message.content); // Retransmit message
                    message.doubleCooldown();
                }
                else {
                    message.incrementCounter();
                }
        }
        }, 500, 500, TimeUnit.MILLISECONDS); // Fixed loop, checks every 500ms
    }

}
