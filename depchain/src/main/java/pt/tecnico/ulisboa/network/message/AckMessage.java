package pt.tecnico.ulisboa.network;

import java.nio.ByteBuffer;

class AckMessage implements Message {
    public static final byte TYPE_INDICATOR = 2;
    
    private final long seqNum;
    
    public AckMessage(long seqNum) {
        this.seqNum = seqNum;
    }
    
    @Override
    public byte getType() {
        return TYPE_INDICATOR;
    }
    
    @Override
    public long getSeqNum() {
        return seqNum;
    }
    
    @Override
    public String getId() {
        return "ACK:" + seqNum;
    }
    
    @Override
    public byte[] serialize() {
        int totalSize = 1 + 8; // type + seqNum
        
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put(TYPE_INDICATOR);
        buffer.putLong(seqNum);
        
        return buffer.array();
    }
    
    public static AckMessage fromByteArray(byte[] data) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            
            if (buffer.remaining() < 8) {
                System.err.println("Error: Insufficient data for AckMessage deserialization.");
                return null;
            }
            
            long seqNum = buffer.getLong();
            
            return new AckMessage(seqNum);
        } catch (Exception e) {
            System.err.println("Error during AckMessage deserialization: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public void printMessage() {
        System.out.println("AckMessage: Sequence Number=" + seqNum + ", Type=" + TYPE_INDICATOR);
    }
}