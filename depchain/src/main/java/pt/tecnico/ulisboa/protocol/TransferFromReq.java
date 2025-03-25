package pt.tecnico.ulisboa.protocol;

// Transfer From request (e.g., for approved spending)
public class TransferFromReq extends ClientReq {
    // TODO: These should all be addresses
    private String spender;    // who sends the request
    private String from;       // transfer from this guy
    private String to;         // transfer to this guy

    private double amount;     // amount to transfer

    public TransferFromReq(Integer id, String signature, long count, 
                           String spender, String from, String to, double amount) {
        super(id, signature, count);
        this.spender = spender;
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.TRANSFER_FROM;
    }

    public String getSpender() { return spender; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public double getAmount() { return amount; }

    @Override
    public String toString() {
        return String.format("TransferFromReq(id=%d, spender=%s, from=%s, to=%s, amount=%.2f)", 
            id, spender, from, to, amount);
    }
}