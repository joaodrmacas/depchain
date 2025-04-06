package pt.tecnico.ulisboa.protocol;

import java.security.PrivateKey;
import java.security.PublicKey;

import com.google.gson.JsonObject;

import pt.tecnico.ulisboa.utils.CryptoUtils;

public abstract class ClientReq extends BlockchainMessage {
    private static final long serialVersionUID = 1L;

    protected int senderId;
    protected String signature;
    protected ClientReqType reqType;

    public ClientReq() {
        // To build from json
        super(BlockchainMessageType.CLIENT_REQ, 0L);
    }

    public ClientReq(int senderId, Long count, ClientReqType reqType) {
        super(BlockchainMessageType.CLIENT_REQ, count);
        this.senderId = senderId;
        this.reqType = reqType;
    }

    public abstract ClientReqType getReqType();

    public int getSenderId() {
        return senderId;
    }

    public String getSignature() {
        return signature;
    }

    protected void setSignature(String signature) {
        this.signature = signature;
    }

    public void sign(PrivateKey privateKey) {
        this.signature = CryptoUtils.signData(this.toString(), privateKey);
    }

    public boolean verifySignature(PublicKey publicKey) {
        return this.signature != null && CryptoUtils.verifySignature(this.toString(), this.signature, publicKey);
    }

    // Enum for request types
    public enum ClientReqType {
        CONTRACT_CALL,
        TRANSFER_DEP_COIN,
        BALANCE_OF_DEP_COIN
    } 

    public boolean isValid() {
        return true;
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

    @Override
    public int hashCode() {
        return 31*super.hashCode() + senderId;
    }
}