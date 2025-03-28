package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

// Transfer From request (e.g., for approved spending)
public class TransferFromReq extends ClientReq {
    private int sender; // who sends the request
    private int from; // transfer from this guy
    private int to; // transfer to this guy
    private BigInteger amount; // amount to transfer

    public TransferFromReq(Integer id, int count, int spender, int from, int to, BigInteger amount) {
        super(id, count);
        this.sender = spender;
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.TRANSFER_FROM;
    }

    public int getSender() {
        return sender;
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
        return String.format("TransferFromReq(id=%d, spender=%d, from=%d, to=%d, amount=%.2f, count=%d)",
                id, sender, from, to, amount, count);
    }
}