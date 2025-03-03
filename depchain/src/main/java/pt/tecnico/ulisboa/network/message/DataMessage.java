package pt.tecnico.ulisboa.network.message;

import java.io.DataInputStream;
import java.io.IOException;

public class DataMessage extends AuthenticatedMessage {
    public static final byte TYPE_INDICATOR = Message.DATA_MESSAGE_TYPE;

    public DataMessage(byte[] content, long seqNum, byte[] hmac) {
        super(content, seqNum, hmac);
    }

    @Override
    public byte getType() {
        return TYPE_INDICATOR;
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