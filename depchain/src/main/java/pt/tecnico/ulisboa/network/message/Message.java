package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class Message {
    public static final byte DATA_MESSAGE_TYPE = 1;
    public static final byte ACK_MESSAGE_TYPE = 2;
    
    private final byte[] content;
    private final int port;
    private final String senderId;
    private final String destinationId;
    private final long seqNum;
    private String key;
    
    // For retransmission mechanism
    private int counter = 1;
    private int cooldown = 1;
    
    public Message(byte[] content, int port, String senderId, String destinationId, long seqNum) {
        this.content = content;
        this.port = port;
        this.senderId = senderId;
        this.destinationId = destinationId;
        this.seqNum = seqNum;
    }
    
    public abstract byte getType();
    
    public abstract byte[] serialize();
    
    public byte[] getContent() {
        return content;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public String getDestinationId() {
        return destinationId;
    }
    
    public long getSeqNum() {
        return seqNum;
    }
    
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }
    
    public void incrementCounter() {
        this.counter++;
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    public void doubleCooldown() {
        this.cooldown *= 2;
    }

    public static Message deserialize(byte[] data) {
        String tipo = "";
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            byte type = dis.readByte();
            switch (type) {
                case DATA_MESSAGE_TYPE:
                    tipo = "Hello";
                    return DataMessage.deserialize(dis);
                case ACK_MESSAGE_TYPE:
                    tipo = "Ack";
                    return AckMessage.deserialize(dis);
                default:
                    System.err.println("Unknown message type: " + type);
                    return null;
            }
        } catch (IOException e) {
            System.err.println("Failed to deserialize message of type: " + tipo + e.getMessage());
            return null;
        }
    }
}