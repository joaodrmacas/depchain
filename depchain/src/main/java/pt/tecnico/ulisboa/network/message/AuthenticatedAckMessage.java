package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AuthenticatedAckMessage extends Message {
    public static final byte TYPE_INDICATOR = Message.AUTHENTICATED_ACK_MESSAGE_TYPE;
    
    private final long seqNum;
    private final String senderId;
    private final byte[] signature;
    
    /**
     * Create a new authenticated acknowledgment message
     */
    public AuthenticatedAckMessage(long seqNum, String senderId, byte[] signature) {
        this.seqNum = seqNum;
        this.senderId = senderId;
        this.signature = signature;
    }
    
    @Override
    public byte getType() {
        return TYPE_INDICATOR;
    }
    
    /**
     * Get the sequence number being acknowledged
     */
    public long getSeqNum() {
        return seqNum;
    }
    
    /**
     * Get the ID of the sender who created this ACK
     */
    public String getSenderId() {
        return senderId;
    }
    
    /**
     * Get the digital signature for this ACK
     */
    public byte[] getSignature() {
        return signature;
    }
    
    @Override
    public byte[] serialize() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            
            dos.writeByte(getType());
            dos.writeLong(seqNum);
            dos.writeUTF(senderId);
            
            // Write signature length and signature
            dos.writeInt(signature.length);
            dos.write(signature);
            
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("Failed to serialize AuthenticatedAckMessage: " + e.getMessage());
            return new byte[0];
        }
    }
    
    /**
     * Deserialize data into an AuthenticatedAckMessage
     */
    public static AuthenticatedAckMessage deserialize(DataInputStream dis) throws IOException {
        long seqNum = dis.readLong();
        String senderId = dis.readUTF();
        
        // Read signature
        int signatureLength = dis.readInt();
        byte[] signature = new byte[signatureLength];
        dis.readFully(signature);
        
        return new AuthenticatedAckMessage(seqNum, senderId, signature);
    }
}

/**
 * Base class for data messages with retransmission functionality
 */
