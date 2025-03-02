package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;


public abstract class Message {
    // Message type indicators used during serialization/deserialization
    public static final byte AUTHENTICATED_DATA_MESSAGE_TYPE = 1;
    public static final byte AUTHENTICATED_ACK_MESSAGE_TYPE = 2;
    
    /**
     * Get the message type indicator
     * @return The type indicator as a byte
     */
    public abstract byte getType();
    
    /**
     * Serialize the message into a byte array
     * @return The serialized message
     */
    public abstract byte[] serialize();
    
    /**
     * Deserialize a byte array back into a Message object
     * @param data The serialized message data
     * @return The deserialized Message object, or null if deserialization fails
     */
    public static Message deserialize(byte[] data) {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            byte type = dis.readByte();
            
            switch (type) {
                case AUTHENTICATED_DATA_MESSAGE_TYPE:
                    return AuthenticatedDataMessage.deserialize(dis);
                case AUTHENTICATED_ACK_MESSAGE_TYPE:
                    return AuthenticatedAckMessage.deserialize(dis);
                default:
                    System.err.println("Unknown message type: " + type);
                    return null;
            }
        } catch (IOException e) {
            System.err.println("Failed to deserialize message: " + e.getMessage());
            return null;
        }
    }
}