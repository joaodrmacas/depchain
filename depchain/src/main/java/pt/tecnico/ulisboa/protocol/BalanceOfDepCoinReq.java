package pt.tecnico.ulisboa.protocol;

import com.google.gson.JsonObject;

public class BalanceOfDepCoinReq extends ClientReq {
    private static final long serialVersionUID = 1L;

    public BalanceOfDepCoinReq() {
        super();
    }

    public BalanceOfDepCoinReq(int senderId, Long count) {
        super(senderId, count, ClientReqType.BALANCE_OF_DEP_COIN);
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.BALANCE_OF_DEP_COIN;
    }

    @Override
    public String toString() {
        return "BalanceDepCoinReq{" +
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
