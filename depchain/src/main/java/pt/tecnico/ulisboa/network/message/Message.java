package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class Message {
    public static final byte DATA_MESSAGE_TYPE = 1;
    public static final byte ACK_MESSAGE_TYPE = 2;

    private final byte[] content;
    private final long seqNum;

    // For retransmission mechanism
    private int counter = 1;
    private int cooldown = 1;

    public Message(byte[] content, long seqNum) {
        this.content = content;
        this.seqNum = seqNum;
    }

    // This is just a way to create a message from a ip:port string -> Duarte
    public Message(byte[] content, String senderId, long seqNum) {
        this.content = content;
        this.seqNum = seqNum;
    }

    public abstract byte getType();

    public abstract byte[] serialize();

    public byte[] getContent() {
        return content;
    }

    public long getSeqNum() {
        return seqNum;
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
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            byte type = dis.readByte();
            switch (type) {
                case DATA_MESSAGE_TYPE:
                    return DataMessage.deserialize(dis);
                case ACK_MESSAGE_TYPE:
                    return AckMessage.deserialize(dis);
                default:
                    System.err.println("Unknown message type: " + type);
                    return null;
            }
        } catch (IOException e) {
            System.err.println("Failed to deserialize message of type: " + e.getMessage());
            return null;
        }
    }
}