package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.utils.types.Logger;

public abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final byte DATA_MESSAGE_TYPE = 1;
    public static final byte ACK_MESSAGE_TYPE = 2;
    public static final byte KEY_MESSAGE_TYPE = 3;
    public static final byte FRAGMENTED_MESSAGE_TYPE = 4;

    private byte[] content;
    private long seqNum;

    // For retransmission mechanism
    private static final int retransmissionTime = Config.RETRANSMISSION_TIME; // in ms
    private int counter = 0; // in ms
    private int cooldown = retransmissionTime; // in ms
    protected int timeout = (int) Math.round(Config.DEFAULT_TIMEOUT); // in ms

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

    public int getTimeout() {
        return this.timeout;
    }

    public void incrementCounter() {
        this.counter += retransmissionTime;
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
            if (dis.available() == 0) {
                Logger.LOG("Empty message received");
                return null;
            }

            byte type = dis.readByte();
            switch (type) {
                case DATA_MESSAGE_TYPE:
                    return DataMessage.deserialize(dis);
                case ACK_MESSAGE_TYPE:
                    return AckMessage.deserialize(dis);
                case KEY_MESSAGE_TYPE:
                    return KeyMessage.deserialize(dis);
                default:
                    Logger.ERROR("Unknown message type: " + type);
                    return null;
            }
        } catch (EOFException e) {
            Logger.LOG("Reached end of stream unexpectedly: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Logger.ERROR("Failed to deserialize message: " + e.getMessage(), e);
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

    public String toStringExtended() {
        String str = "{";
        switch (this.getType()) {
            case Message.DATA_MESSAGE_TYPE:
                str = "DT";
                break;
            case Message.ACK_MESSAGE_TYPE:
                str = "ACK";
                break;
            case Message.KEY_MESSAGE_TYPE:
                str = "KEY";
                break;
            default:
                str = "UNKNOWN";
                break;
        }

        if (str == "DT") {
            str += "{" + (DataMessage) this + "}";
        }

        str += ", " + this.getSeqNum() + "}";

        return str;
    }
}