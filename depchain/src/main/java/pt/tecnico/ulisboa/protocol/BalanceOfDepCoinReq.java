package pt.tecnico.ulisboa.protocol;

public class BalanceOfDepCoinReq extends ClientReq {
    private static final long serialVersionUID = 1L;

    public BalanceOfDepCoinReq(int senderId, Long count) {
        super(senderId, count);
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
    public boolean needsConsensus() {
        return false;
    }

}
