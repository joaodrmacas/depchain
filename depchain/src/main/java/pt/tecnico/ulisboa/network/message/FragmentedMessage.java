package pt.tecnico.ulisboa.network.message;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class FragmentedMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private byte[] fragmentData;
    private int fragmentIndex;
    private int totalFragments;
    private int originalMessageSize;
    private String messageId; //senderId + seqnum

    public FragmentedMessage(byte[] fragmentData, int fragmentIndex, int totalFragments, int originalMessageSize, String messageId) {
        this.fragmentData = fragmentData;
        this.fragmentIndex = fragmentIndex;
        this.totalFragments = totalFragments;
        this.originalMessageSize = originalMessageSize;
        this.messageId = messageId;
    }

    public byte[] getFragmentData() {
        return fragmentData;
    }

    public String getMessageId() {
        return messageId;
    }
    
    public int getFragmentIndex() {
        return fragmentIndex;
    }
    
    public int getTotalFragments() {
        return totalFragments;
    }

    public int getOriginalMessageSize() {
        return originalMessageSize;
    }

    public static byte[] reassembleFragments(FragmentedMessage[] fragments) {
        
        // Reassemble
        ByteBuffer buffer = ByteBuffer.allocate(fragments[0].getOriginalMessageSize());
        for (FragmentedMessage fragment : fragments) {
            buffer.put(fragment.getFragmentData());
        }
        
        return buffer.array();
    }
    
}