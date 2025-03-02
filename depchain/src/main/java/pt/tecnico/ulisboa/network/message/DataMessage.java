package pt.tecnico.ulisboa.network.message;

public abstract class DataMessage extends Message {
    private final String destination;
    private final int port;
    private final byte[] content;
    private final long seqNum;
    private String key;
    
    // For retransmission mechanism
    private int counter = 1;
    private int cooldown = 1;
    
    public DataMessage(String destination, int port, byte[] content, long seqNum) {
        this.destination = destination;
        this.port = port;
        this.content = content;
        this.seqNum = seqNum;
    }
    
    /**
     * Get the destination address
     */
    public String getDestination() {
        return destination;
    }
    
    /**
     * Get the destination port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Get the message content
     */
    public byte[] getContent() {
        return content;
    }
    
    /**
     * Get the sequence number
     */
    public long getSeqNum() {
        return seqNum;
    }
    
    /**
     * Get the unique message key for tracking
     */
    public String getKey() {
        return key;
    }
    
    /**
     * Set the unique message key for tracking
     */
    public void setKey(String key) {
        this.key = key;
    }
    
    /**
     * Get the current counter value for retransmission
     */
    public int getCounter() {
        return counter;
    }
    
    /**
     * Set the counter value for retransmission
     */
    public void setCounter(int counter) {
        this.counter = counter;
    }
    
    /**
     * Increment the counter value
     */
    public void incrementCounter() {
        this.counter++;
    }
    
    /**
     * Get the current cooldown period
     */
    public int getCooldown() {
        return cooldown;
    }
    
    /**
     * Double the cooldown period for exponential backoff
     */
    public void doubleCooldown() {
        this.cooldown *= 2;
    }
}