package pt.tecnico.ulisboa.protocol;

public class TransferDepCoinReq extends ClientReq {
    private static final long serialVersionUID = 1L;

    private Address receiver;
    private int amount;

    public TransferDepCoinReq(int senderId, Long count, Address receiver, int amount) {
        super(senderId, count);
        this.receiver = receiver;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.CONTRACT_CALL;
    }

    public String getReceiver() {
        return receiver;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "TransferDepCoinReq{" +
                "receiver='" + receiver + '\'' +
                ", amount=" + amount +
                ", senderId=" + senderId +
                ", count=" + count +
                '}';
    }
    
}
