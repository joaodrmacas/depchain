package pt.tecnico.ulisboa.network.message;

import java.io.Serializable;

//TODO: move the authentication of the message to here?
public class FragmentedMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private byte[] fragmentData;
    private int fragmentIndex;
    private int totalFragments;
    private String messageId; //senderId + seqnum

    public FragmentedMessage(byte[] fragmentData, int fragmentIndex, int totalFragments, int originalMessageSize, String messageId) {
        this.fragmentData = fragmentData;
        this.fragmentIndex = fragmentIndex;
        this.totalFragments = totalFragments;
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
    
}