package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

// Transfer request
public class BuyISTReq extends ClientReq {
    private BigInteger amount; // amount to buy

    public BuyISTReq(int senderId, Long count, BigInteger amount) {
        super(senderId, count);
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.BUY_IST;
    }

    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return String.format("TransferReq(senderId(buyer)=%d, amount=%.2f, count=%d)",
                senderId, amount, count);
    }
}