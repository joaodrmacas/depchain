package pt.tecnico.ulisboa.protocol;

import java.io.Serializable;
import com.google.gson.JsonObject;

public abstract class BlockchainMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private BlockchainMessageType type;
    protected Long count;

    public BlockchainMessage() {
        // To build from json
    }

    public BlockchainMessage(BlockchainMessageType type, Long count) {
        this.type = type;
        this.count = count;
    }

    public static enum BlockchainMessageType {
        CLIENT_REQ,
        CLIENT_RESP,
        REGISTER_REQ,
        REGISTER_RESP,
    }

    public BlockchainMessageType getType() {
        return type;
    }

    public Long getCount() {
        return count;
    }

    public int getSenderId() {
        return -1; // TODO: isto está insanely disgusting mas tenho cpd para fazer - fix this
    }

    @Override
    public boolean equals(Object obj) {
        // TODO: acho que isto ta mal nao?
        if (obj instanceof BlockchainMessage) {
            BlockchainMessage other = (BlockchainMessage) obj;
            return type.equals(other.type) && count.equals(other.count);
        }
        return false;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.toString());
        json.addProperty("count", count);
        return json;
    }

    public void fromJson(JsonObject json) {
        type = BlockchainMessageType.valueOf(json.get("type").getAsString());
        count = json.get("count").getAsLong();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + type.hashCode();
        result = 31 * result + count.hashCode();
        return result;
    }
}