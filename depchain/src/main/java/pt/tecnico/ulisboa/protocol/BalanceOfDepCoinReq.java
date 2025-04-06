package pt.tecnico.ulisboa.protocol;

import com.google.gson.JsonObject;

public class BalanceOfDepCoinReq extends ClientReq {
    private static final long serialVersionUID = 1L;

    private String ofAddr;

    public BalanceOfDepCoinReq() {
        super();
    }

    public BalanceOfDepCoinReq(int senderId, Long count, String ofAddr) {
        super(senderId, count, ClientReqType.BALANCE_OF_DEP_COIN);
        this.ofAddr = ofAddr;
    }

    public String getAddress() {
        return ofAddr;
    }

    public void setAddress(String ofAddr) {
        this.ofAddr = ofAddr;
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.BALANCE_OF_DEP_COIN;
    }

    @Override
    public String toString() {
        return "BalanceOfDepCoinReq{" +
                "ofAddr='" + ofAddr + '\'' +
                ", senderId=" + senderId +
                ", count=" + count +
                '}';
    }

    @Override
    public void fromJson(JsonObject json) {
        super.fromJson(json);
    }

    @Override
    public JsonObject toJson() {
        return super.toJson();
    }

}
