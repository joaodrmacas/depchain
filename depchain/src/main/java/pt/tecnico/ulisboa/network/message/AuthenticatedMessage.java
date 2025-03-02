package pt.tecnico.ulisboa.network.message;

import java.io.*;
import java.util.Arrays;

public class AuthenticatedMessage extends Message {

    private final byte[] content;
    private final byte[] mac;
    private final String senderId;
    private long seqNum;

    public AuthenticatedMessage(byte[] content, byte[] mac, String senderId) {
        this.content = content;
        this.mac = mac;
        this.senderId = senderId;
        this.seqNum = 0;
    }

    public AuthenticatedMessage(byte[] content, byte[] mac, String senderId, long seqNum) {
        this.content = content;
        this.mac = mac;
        this.senderId = senderId;
        this.seqNum = seqNum;
    }

    @Override
    public byte getType() {
        return AUTH_TYPE;
    }

    @Override
    public long getSeqNum() {
        return seqNum;
    }

    @Override
    public String getId() {
        return senderId + ":" + Arrays.hashCode(content);
    }

    public byte[] getContent() {
        return content;
    }

    public byte[] getMac() {
        return mac;
    }

    public String getSenderId() {
        return senderId;
    }

    @Override
    public byte[] serialize() {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            byteStream.write(getType());
            try (DataOutputStream dataStream = new DataOutputStream(byteStream)) {
                // Write sender ID
                dataStream.writeUTF(senderId);
                // Write sequence number
                dataStream.writeLong(seqNum);
                // Write content
                dataStream.writeInt(content.length);
                dataStream.write(content);
                // Write MAC
                dataStream.writeInt(mac.length);
                dataStream.write(mac);

                return byteStream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static AuthenticatedMessage fromByteArray(byte[] data) {
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
            // Skip the type byte since we already know what type it is
            byteStream.read();
            DataInputStream dataStream = new DataInputStream(byteStream);
            // Read sender ID
            String senderId = dataStream.readUTF();
            // Read sequence number
            long seqNum = dataStream.readLong();
            // Read content
            int contentLength = dataStream.readInt();
            byte[] content = new byte[contentLength];
            dataStream.readFully(content);
            // Read MAC
            int macLength = dataStream.readInt();
            byte[] mac = new byte[macLength];
            dataStream.readFully(mac);
            return new AuthenticatedMessage(content, mac, senderId, seqNum);
        } catch (IOException e) {
            System.err.println("Error during AuthenticatedMessage deserialization: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void printMessage() {
        System.out.println("AuthenticatedMessage: SenderId=" + senderId +
                ", Sequence Number=" + seqNum +
                ", Content Length=" + (content != null ? content.length : 0) +
                ", MAC Length=" + (mac != null ? mac.length : 0) +
                ", Type=" + getType());
    }
}
