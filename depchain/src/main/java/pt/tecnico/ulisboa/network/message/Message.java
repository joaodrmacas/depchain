package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;

import pt.tecnico.ulisboa.Config;

public abstract class Message implements Serializable {
    public static final byte DATA_MESSAGE_TYPE = 1;
    public static final byte ACK_MESSAGE_TYPE = 2;
    public static final byte KEY_MESSAGE_TYPE = 3;

    private byte[] content;
    private long seqNum;

    // For retransmission mechanism
    private int counter = 1;
    private int cooldown = 1;
    protected int timeout = (int) Math.round(Config.DEFAULT_TIMEOUT * 0.05);

    public Message(byte[] content, long seqNum) {
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

    public void setSeqNum(long seqNum) {
        this.seqNum = seqNum;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public int getTimeout(){
        return this.timeout;
    }

    public void incrementCounter() {
        this.counter++;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void doubleCooldown() {
        if (this.cooldown < Config.MAX_COOLDOWN)
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
                case KEY_MESSAGE_TYPE:
                    return KeyMessage.deserialize(dis);
                default:
                    System.err.println("Unknown message type: " + type);
                    return null;
            }
        } catch (IOException e) {
            System.err.println("Failed to deserialize message of type: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + getType() +
                ", seqNum=" + seqNum +
                ", content=" + new String(content) +
                '}';
    }
}