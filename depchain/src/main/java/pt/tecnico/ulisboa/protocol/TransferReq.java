package pt.tecnico.ulisboa.protocol;

// Transfer request
public class TransferReq extends ClientReq {
    // TODO: These should all be addresses
    private String from;     // transfer from this guy
    private String to;       // transfer to this guy

    private double amount;   // amount to transfer

    public TransferReq(Integer id, String signature, long count, String from, String to, double amount) {
        super(id, signature, count);
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.TRANSFER;
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public double getAmount() { return amount; }

    @Override
    public String toString() {
        return String.format("TransferReq(id=%d, from=%s, to=%s, amount=%.2f)", 
            id, from, to, amount);
    }
}