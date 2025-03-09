package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.ObjectInputStream;


public class KeyMessage extends Message {
    public static final byte TYPE_INDICATOR = Message.KEY_MESSAGE_TYPE;

    public KeyMessage(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super();
        readObject(in);
    }

    public KeyMessage(byte[] key, long seqNum) {
        super(key, seqNum);
    }

    public byte getType(){
        return TYPE_INDICATOR;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        byte type = in.readByte();
        if (type != TYPE_INDICATOR) {
            throw new IOException("Invalid message type: " + type);
        }
        
        long seqNum = in.readLong();
        int contentLength = in.readInt();
        byte[] content = new byte[contentLength];
        in.readFully(content);
        
        //int hmacLength = in.readInt();
        //byte[] hmac = new byte[hmacLength];
        //in.readFully(hmac);
        
        setSeqNum(seqNum);
        setContent(content);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeByte(getType());
        out.writeLong(getSeqNum());
        out.writeInt(getContent().length);
        out.write(getContent());
        //out.writeInt(getMac().length);
        //out.write(getMac());
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