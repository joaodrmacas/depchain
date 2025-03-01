package pt.tecnico.ulisboa.network;

import java.nio.ByteBuffer;

class AckMessage implements Message {
    public static final byte TYPE_INDICATOR = 2;
    
    private final String destination;
    private final int port;
    private final long seqNum;
    
    public AckMessage(String destination, int port, long seqNum) {
        this.destination = destination;
        this.port = port;
        this.seqNum = seqNum;
    }
    
    @Override
    public byte getType() {
        return TYPE_INDICATOR;
    }
    
    @Override
    public String getDestination() {
        return destination;
    }
    
    @Override
    public int getPort() {
        return port;
    }
    
    @Override
    public long getSeqNum() {
        return seqNum;
    }
    
    @Override
    public String getId() {
        return destination + ":" + port + ":ACK:" + seqNum;
    }
    
    @Override
    public byte[] serialize() {
        byte[] destBytes = destination.getBytes();
        int totalSize = 1 + 4 + destBytes.length + 4 + 8; // type + stringLength + string + port + seqNum
        
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put(TYPE_INDICATOR);
        buffer.putInt(destBytes.length);
        buffer.put(destBytes);
        buffer.putInt(port);
        buffer.putLong(seqNum);
        
        return buffer.array();
    }
    
    public static AckMessage fromByteArray(byte[] data) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            
            int destLength = buffer.getInt();
            
            if (buffer.remaining() < destLength + 12) {
                System.err.println("Error: Insufficient data for AckMessage deserialization.");
                return null;
            }
            
            byte[] destBytes = new byte[destLength];
            buffer.get(destBytes);
            String destination = new String(destBytes);
            
            int port = buffer.getInt();
            long seqNum = buffer.getLong();
            
            return new AckMessage(destination, port, seqNum);
        } catch (Exception e) {
            System.err.println("Error during AckMessage deserialization: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void printMessage() {
		System.out.println("AckMessage: Destination=" + destination + ", Port=" + port + ", Sequence Number=" + seqNum + ", Type=" + TYPE_INDICATOR);
    }
}