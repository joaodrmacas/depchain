package pt.tecnico.ulisboa.network.message;

import java.io.DataInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class DataMessage extends AuthenticatedMessage {
    public static final byte TYPE_INDICATOR = Message.DATA_MESSAGE_TYPE;
    
    public DataMessage(byte[] content, int port, String senderId, String destinationId, 
                        long seqNum, byte[] hmac) {
        super(content, port, senderId, destinationId, seqNum, hmac);
    }
    
    public DataMessage(byte[] content, int port, String senderId, String destinationId, 
                        long seqNum, Key secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        super(content, port, senderId, destinationId, seqNum, secretKey);
    }
    
    @Override
    public byte getType() {
        return TYPE_INDICATOR;
    }
    
    public static DataMessage deserialize(DataInputStream dis) throws IOException {    
        // Read fields in the correct order
        String senderId = dis.readUTF();
        String destinationId = dis.readUTF();
        long seqNum = dis.readLong();
        int port = dis.readInt();
    
        // Read content length
        int contentLength = dis.readInt();
        byte[] content = new byte[contentLength];
        dis.readFully(content);
    
        // Read HMAC
        int hmacLength = dis.readInt();
        byte[] hmac = new byte[hmacLength];
        dis.readFully(hmac);
    
        return new DataMessage(content, port, senderId, destinationId, seqNum, hmac);
    }
    
}