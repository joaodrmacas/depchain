ackage pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class AuthenticatedDataMessage extends DataMessage {
    public static final byte TYPE_INDICATOR = Message.AUTHENTICATED_DATA_MESSAGE_TYPE;
    
    private String senderId;
    private byte[] signature;
    
    /**
     * Create a new authenticated data message
     */
    public AuthenticatedDataMessage(String destination, int port, byte[] content,
                                  long seqNum, String senderId, byte[] signature) {
        super(destination, port, content, seqNum);
        this.senderId = senderId;
        this.signature = signature;
    }
    
    @Override
    public byte getType() {
        return TYPE_INDICATOR;
    }
    
    /**
     * Get the ID of the sender who created this message
     */
    public String getSenderId() {
        return senderId;
    }
    
    /**
     * Get the digital signature for this message
     */
    public byte[] getSignature() {
        return signature;
    }
    
    @Override
    public byte[] serialize() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            
            dos.writeByte(getType());
            dos.writeUTF(getDestination());
            dos.writeInt(getPort());
            dos.writeLong(getSeqNum());
            
            // Write content length and content
            dos.writeInt(getContent().length);
            dos.write(getContent());
            
            // Write sender ID
            dos.writeUTF(senderId);
            
            // Write signature length and signature
            dos.writeInt(signature.length);
            dos.write(signature);
            
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            System.err.println("Failed to serialize AuthenticatedDataMessage: " + e.getMessage());
            return new byte[0];
        }
    }
    
    /**
     * Deserialize data into an AuthenticatedDataMessage
     */
    public static AuthenticatedDataMessage deserialize(DataInputStream dis) throws IOException {
        String destination = dis.readUTF();
        int port = dis.readInt();
        long seqNum = dis.readLong();
        
        // Read content
        int contentLength = dis.readInt();
        byte[] content = new byte[contentLength];
        dis.readFully(content);
        
        // Read sender ID
        String senderId = dis.readUTF();
        
        // Read signature
        int signatureLength = dis.readInt();
        byte[] signature = new byte[signatureLength];
        dis.readFully(signature);
        
        return new AuthenticatedDataMessage(destination, port, content, seqNum, senderId, signature);
    }
}
