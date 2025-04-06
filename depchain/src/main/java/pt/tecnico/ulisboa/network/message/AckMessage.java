package pt.tecnico.ulisboa.network.message;

import java.io.DataInputStream;
import java.io.IOException;

public class AckMessage extends AuthenticatedMessage {
    public static final byte TYPE_INDICATOR = Message.ACK_MESSAGE_TYPE;

    public AckMessage(long seqNum, byte[] hmac) {
        super(new byte[0], seqNum, hmac);
    }

    @Override
    public byte getType() {
        return TYPE_INDICATOR;
    }

    public static AckMessage deserialize(DataInputStream dis) throws IOException {
        long seqNum = dis.readLong();

        // Read content
        int contentLength = dis.readInt();
        byte[] content = new byte[contentLength];
        dis.readFully(content);

        // Read HMAC
        int hmacLength = dis.readInt();
        byte[] hmac = new byte[hmacLength];
        dis.readFully(hmac);

        return new AckMessage(seqNum, hmac);
    }
}