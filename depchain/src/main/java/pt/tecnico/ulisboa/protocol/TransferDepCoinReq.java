package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

import com.google.gson.JsonObject;

import org.hyperledger.besu.datatypes.Address;

public class TransferDepCoinReq extends ClientReq {
    private static final long serialVersionUID = 1L;

    private String receiverAddr;
    private BigInteger amount;

     public TransferDepCoinReq() {
        // For json
        super();
    }

    public TransferDepCoinReq(int senderId, Long count, String receiver, BigInteger amount) {
        super(senderId, count, ClientReqType.TRANSFER_DEP_COIN);
        this.receiverAddr = receiver;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.TRANSFER_DEP_COIN;
    }

    public Address getReceiver() {
        return Address.fromHexString(receiverAddr);
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setReceiver(String receiver) {
        this.receiverAddr = receiver;
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    public String toString() {
        return "TransferDepCoinReq{" +
                "receiver='" + receiverAddr + '\'' +
                ", amount=" + amount +
                ", senderId=" + senderId +
                ", count=" + count +
                '}';
    }

    public void fromJson(JsonObject json) {
        super.fromJson(json);
        this.receiverAddr = json.get("receiverAddr").getAsString();
        this.amount = new BigInteger(json.get("amount").getAsString());
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("receiverAddr", receiverAddr);
        json.addProperty("amount", amount.toString());
        return json;
    }
}
