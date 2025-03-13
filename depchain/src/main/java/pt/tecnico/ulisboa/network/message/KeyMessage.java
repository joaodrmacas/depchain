package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class KeyMessage extends Message {
    public static final byte TYPE_INDICATOR = Message.KEY_MESSAGE_TYPE;

    public KeyMessage(byte[] key, long seqNum) {
        super(key, seqNum);
    }

    public byte getType() {
        return TYPE_INDICATOR;
    }

    @Override
    public byte[] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write message type
            dos.writeByte(getType());

            // Write message fields
            dos.writeLong(getSeqNum());

            // Write content
            dos.writeInt(getContent().length);
            dos.write(getContent());

            dos.flush();

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    public static KeyMessage deserialize(DataInputStream dis) {
        try {

            // Read message fields
            long seqNum = dis.readLong();

            // Read content
            int contentLength = dis.readInt();
            byte[] content = new byte[contentLength];
            dis.readFully(content);

            return new KeyMessage(content, seqNum);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize message", e);
        }
    }

}