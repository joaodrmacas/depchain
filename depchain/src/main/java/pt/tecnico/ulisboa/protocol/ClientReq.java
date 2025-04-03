package pt.tecnico.ulisboa.protocol;

import com.google.gson.JsonObject;

public abstract class ClientReq extends BlockchainMessage {
    private static final long serialVersionUID = 1L;

    protected int senderId;
    protected String signature;
    protected ClientReqType reqType;

    public ClientReq() {
        // To build from json
        super(BlockchainMessageType.CLIENT_REQ, 0L);
    }

    public ClientReq(int senderId, Long count,ClientReqType reqType) {
        super(BlockchainMessageType.CLIENT_REQ, count);
        this.senderId = senderId;
        this.reqType = reqType;
    }

    public abstract ClientReqType getReqType();

    @Override
    public int getSenderId() {
        return senderId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    // Enum for request types
    public enum ClientReqType {
        CONTRACT_CALL,
        TRANSFER_DEP_COIN,
        BALANCE_OF_DEP_COIN
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("senderId", senderId);
        if (signature != null) {
            json.addProperty("signature", signature);
        }
        json.addProperty("reqType", getReqType().toString());
        return json;
    }

    @Override
    public void fromJson(JsonObject json) {
        super.fromJson(json);
        this.senderId = json.get("senderId").getAsInt();
        if (json.has("signature")) {
            this.signature = json.get("signature").getAsString();
        }
        this.reqType = ClientReqType.valueOf(json.get("reqType").getAsString());
    }

    

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ClientReq)) {
            return false;
        }
        ClientReq other = (ClientReq) obj;
        return super.equals(obj) && senderId == other.senderId;
    }

}