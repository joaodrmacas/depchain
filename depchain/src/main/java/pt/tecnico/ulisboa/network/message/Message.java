package pt.tecnico.ulisboa.network;

import java.io.*;
import java.nio.ByteBuffer;

public interface Message {
    byte[] serialize();
    long getSeqNum();
    String getId();
    void printMessage();
    
    static Message deserialize(byte[] data) {
        if (data == null || data.length == 0) {
            System.err.println("Error: Attempted to deserialize null or empty data.");
            return null;
        }
        
        if (data.length > 0) {
            byte messageType = data[0];
            byte[] messageData = new byte[data.length - 1];
            System.arraycopy(data, 1, messageData, 0, messageData.length);
            
            switch (messageType) {
                case DataMessage.TYPE_INDICATOR:
                    return DataMessage.fromByteArray(messageData);
                case AckMessage.TYPE_INDICATOR:
                    return AckMessage.fromByteArray(messageData);
                default:
                    System.err.println("Unknown message type: " + messageType);
                    return null;
            }
        }
        return null;
    }
    
    byte getType();
}