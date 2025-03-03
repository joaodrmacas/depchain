package pt.tecnico.ulisboa.network.message;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;

public class AuthenticatedMessage extends Message {
    private final byte[] hmac;
    
    public AuthenticatedMessage(byte[] content, int port, String senderId, String destinationId, 
                               long seqNum, byte[] hmac) {
        super(content, port, senderId, destinationId, seqNum);
        this.hmac = hmac;  // Fixed the missing assignment
    }
    
    public AuthenticatedMessage(byte[] content, int port, String senderId, String destinationId, 
                               long seqNum, Key secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        super(content, port, senderId, destinationId, seqNum);
        this.hmac = generateMac(secretKey);
    }
    
    public byte[] getMac() {
        return hmac;
    }

    @Override
    public byte getType() {
        return Message.DATA_MESSAGE_TYPE;
    }

    @Override
    public byte[] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            // Write message type
            dos.writeByte(getType());
            
            // Write message fields
            dos.writeUTF(getSenderId());
            dos.writeUTF(getDestinationId());
            dos.writeLong(getSeqNum());
            dos.writeInt(getPort());

            // Write content
            dos.writeInt(getContent().length);
            dos.write(getContent());
            
            // Write MAC
            dos.writeInt(hmac.length);
            dos.write(hmac);

            dos.flush();
            
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    protected byte[] generateMac(Key secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getEncoded(), "HmacSHA256"));
        
        ByteArrayOutputStream dataToAuthenticate = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(dataToAuthenticate)) {
            dos.writeUTF(getSenderId());
            
            dos.writeUTF(getDestinationId());
            
            dos.writeLong(getSeqNum());
            
            dos.writeInt(getPort());
            
            dos.write(getContent());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate data for MAC", e);
        }
        
        return mac.doFinal(dataToAuthenticate.toByteArray());
    }
    
    public boolean verifyMac(Key secretKey) {
        try {
            byte[] expectedMac = generateMac(secretKey);
            
            // Compare MACs for equality
            if (hmac.length != expectedMac.length) {
                return false;
            }
            
            // Constant-time comparison to prevent timing attacks
            int result = 0;
            for (int i = 0; i < hmac.length; i++) {
                result |= hmac[i] ^ expectedMac[i];
            }
            
            return result == 0;
        } catch (Exception e) {
            System.err.println("MAC verification failed: " + e.getMessage());
            return false;
        }
    }
    
}