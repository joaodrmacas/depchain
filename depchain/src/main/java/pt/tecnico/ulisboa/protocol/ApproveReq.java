package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

// Approve request to grant spending allowance
public class ApproveReq extends ClientReq {
    private int allower; // this guy approves the spending
    private int allowee; // this guy is allowed to spend
    private BigInteger amount; // amount to allow

    public ApproveReq(Integer id, int count, int allower, int allowee, BigInteger amount) {
        super(id, count);
        this.allower = allower;
        this.allowee = allowee;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.APPROVE;
    }

    public int getAllower() {
        return allower;
    }

    public int getAllowee() {
        return allowee;
    }

    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return String.format("ApproveReq(id=%d, allower=%d, allowee=%d, amount=%.2f, count=%d)",
                id, allower, allowee, amount, count);
    }
}