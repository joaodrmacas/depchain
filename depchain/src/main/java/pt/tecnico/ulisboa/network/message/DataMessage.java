package pt.tecnico.ulisboa.network.message;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class DataMessage extends AuthenticatedMessage {
    private static final long serialVersionUID = 1L;
    public static final byte TYPE_INDICATOR = Message.DATA_MESSAGE_TYPE;

    public DataMessage(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super();
        readObject(in);
    }

    public DataMessage(byte[] content, long seqNum, byte[] hmac) {
        super(content, seqNum, hmac);
    }

    @Override
    public byte getType() {
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
        
        int hmacLength = in.readInt();
        byte[] hmac = new byte[hmacLength];
        in.readFully(hmac);
        
        setSeqNum(seqNum);
        setContent(content);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeByte(getType());
        out.writeLong(getSeqNum());
        out.writeInt(getContent().length);
        out.write(getContent());
        out.writeInt(getMac().length);
        out.write(getMac());
    }

    // TODO this method is exactly the same as the one in AckMessage, maybe move to
    // the parent and find a way to return the correct type
    public static DataMessage deserialize(DataInputStream dis) throws IOException {
        
        long seqNum = dis.readLong();

        // Read content
        int contentLength = dis.readInt();
        byte[] content = new byte[contentLength];
        dis.readFully(content);

        // Read HMAC
        int hmacLength = dis.readInt();
        byte[] hmac = new byte[hmacLength];
        dis.readFully(hmac);

        return new DataMessage(content, seqNum, hmac);
    }

}