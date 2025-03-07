package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.ObjectInputStream;


public class KeyMessage extends Message {
    public static final byte TYPE_INDICATOR = Message.KEY_MESSAGE_TYPE;

    public KeyMessage(byte[] key, long seqNum) {
        super(key, seqNum);
    }

    public byte getType(){
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

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        // We don't call defaultWriteObject() here since we're fully customizing the serialization
        
        // Write message type
        out.writeByte(getType());
        
        // Write sequence number
        out.writeLong(getSeqNum());
        
        // Write content length and content
        out.writeInt(getContent().length);
        out.write(getContent());
        
        // Write retransmission fields
        out.writeInt(getCounter());
        out.writeInt(getCooldown());
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

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Skip type (we already know it's a KeyMessage)
        in.readByte();
        
        // Read sequence number
        long seqNum = in.readLong();
        
        // Read content
        int contentLength = in.readInt();
        byte[] content = new byte[contentLength];
        in.readFully(content);
        
        // Read retransmission fields
        int counter = in.readInt();
        int cooldown = in.readInt();
        
        // Use setters to update the fields
        setSeqNum(seqNum);
        setContent(content);
        setCounter(counter);
        setCooldown(cooldown);
    }

}