package pt.tecnico.ulisboa.protocol;

import java.util.Date;

import pt.tecnico.ulisboa.utils.RequiresEquals;

public class BlockchainResponse<T extends RequiresEquals> extends BlockchainMessage<T> {
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private Date timestamp; //TODO: is this needed?

    public BlockchainResponse(boolean success, Date timestamp) {
        super(MessageType.CLIENT_RESPONSE);
        this.success = success;
        this.timestamp = timestamp;
    }

    public boolean getSuccess() {
        return success;
    }

    public Date getTimestamp() {
        return timestamp;
    }

}