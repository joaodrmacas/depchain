package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
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

    protected Message() {
        this.counter = 1;
        this.cooldown = 1;
    }

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

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeByte(getType());
        out.writeLong(getSeqNum());
        out.writeInt(content.length);
        out.write(content);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.readByte();
        this.seqNum = in.readLong();
        int contentLength = in.readInt();
        this.content = new byte[contentLength];
        in.readFully(this.content);
    }

    public static Message deserializeFromStream(ObjectInputStream in) throws IOException, ClassNotFoundException {
        byte type = in.readByte(); // Peek at type
        in.reset(); // Reset stream position
        
        switch (type) {
            case DATA_MESSAGE_TYPE:
                return new DataMessage(in);
            case ACK_MESSAGE_TYPE:
                return new AckMessage(in);
            case KEY_MESSAGE_TYPE:
                return new KeyMessage(in);
            default:
                throw new IOException("Unknown message type: " + type);
        }
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