package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

// Transfer From request (e.g., for approved spending)
public class TransferFromReq extends ClientReq {
    // TODO: These should all be addresses
    private String spender; // who sends the request
    private String from; // transfer from this guy
    private String to; // transfer to this guy

    private BigInteger amount; // amount to transfer

    public TransferFromReq(Integer id, long count, String spender, String from, String to, BigInteger amount) {
        super(id, count);
        this.spender = spender;
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.TRANSFER_FROM;
    }

    public String getSpender() {
        return spender;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return String.format("TransferFromReq(id=%d, spender=%s, from=%s, to=%s, amount=%.2f, count=%d)",
                id, spender, from, to, amount, count);
    }
}