package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

// Transfer From request (e.g., for approved spending)
public class TransferFromReq extends ClientReq {
    private int from; // transfer from this guy
    private int to; // transfer to this guy

    private BigInteger amount; // amount to transfer

    public TransferFromReq(int senderId, Long count, int from, int to, BigInteger amount) {
        super(senderId, count);
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.TRANSFER_FROM;
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
        return String.format("TransferFromReq(senderId=%d, from=%d, to=%d, amount=%.2f, count=%d)",
                senderId, from, to, amount, count);
    }
}