package pt.tecnico.ulisboa.network.message;

import java.io.DataInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class AckMessage extends AuthenticatedMessage {
    public static final byte TYPE_INDICATOR = Message.ACK_MESSAGE_TYPE;
    
    public AckMessage(int port, String senderId, String destinationId, long seqNum, byte[] hmac) {
        super("".getBytes(), port, senderId, destinationId, seqNum, hmac);
    }
    
    public AckMessage(int port, String senderId, String destinationId, 
                      long seqNum, Key secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        super("".getBytes(), port, senderId, destinationId, seqNum, secretKey);
    }
    
    public static AckMessage createFromMessage(Message receivedMessage, String newSenderId, Key secretKey) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        return new AckMessage(
            receivedMessage.getPort(),
            newSenderId,
            receivedMessage.getSenderId(), // Switch the destination to be the original sender
            receivedMessage.getSeqNum(),
            secretKey
        );
    }

    @Override
    public byte getType() {
        return TYPE_INDICATOR;
    }
    
    public static AckMessage deserialize(DataInputStream dis) throws IOException {    
        String senderId = dis.readUTF();
        System.out.println("Sender ID: " + senderId);
        String destinationId = dis.readUTF();
        System.out.println("Destination ID: " + destinationId);
        long seqNum = dis.readLong();
        System.out.println("SeqNum: " + seqNum);
        int port = dis.readInt();
        System.out.println("Port: " + port);
    
        // Read content length
        int contentLength = dis.readInt();
        System.out.println("Content Length: " + contentLength);
        byte[] content = new byte[contentLength];
        dis.readFully(content);
        System.out.println("Content: " + new String(content));
    
        // Read HMAC
        int hmacLength = dis.readInt();
        System.out.println("HMAC Length: " + hmacLength);
        byte[] hmac = new byte[hmacLength];
        dis.readFully(hmac);
        System.out.println("HMAC: " + new String(hmac));
    
        return new AckMessage(port, senderId, destinationId, seqNum, hmac);
    }
}