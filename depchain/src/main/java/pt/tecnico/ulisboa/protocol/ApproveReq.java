package pt.tecnico.ulisboa.protocol;

// Approve request to grant spending allowance
public class ApproveReq extends ClientReq {
    // TODO: These should all be addresses
    private String owner; // this guy approves the spending
    private String spender; // this guy is allowed to spend

    private double amount; // amount to allow

    public ApproveReq(Integer id, long count,
            String owner, String spender, double amount) {
        super(id, count);
        this.owner = owner;
        this.spender = spender;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.APPROVE;
    }

    public String getOwner() {
        return owner;
    }

    public String getSpender() {
        return spender;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return String.format("ApproveReq(id=%d, owner=%s, spender=%s, amount=%.2f, count=%d)",
                id, owner, spender, amount, count);
    }
}