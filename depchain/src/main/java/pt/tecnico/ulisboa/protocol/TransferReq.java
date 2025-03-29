package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

// Transfer request
public class TransferReq extends ClientReq {
    private int to; // transfer to this guy
 
    private BigInteger amount; // amount to transfer

    public TransferReq(int senderId, Long count, int to, BigInteger amount) {
        super(senderId, count);
        this.to = to;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.TRANSFER;
    }

    public int getTo() {
        return to;
    }

    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return String.format("TransferReq(senderId(from)=%d, to=%d, amount=%.2f, count=%d)",
                senderId, to, amount, count);
    }
}