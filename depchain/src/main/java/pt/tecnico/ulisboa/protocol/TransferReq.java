package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

// Transfer request
public class TransferReq extends ClientReq {
    private int from; // transfer from this guy
    private int to; // transfer to this guy
    private BigInteger amount; // amount to transfer

    public TransferReq(Integer id, int count, int from, int to, BigInteger amount) {
        super(id, count);
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.TRANSFER;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return String.format("TransferReq(id=%d, from=%d, to=%d, amount=%.2f, count=%d)",
                id, from, to, amount, count);
    }
}