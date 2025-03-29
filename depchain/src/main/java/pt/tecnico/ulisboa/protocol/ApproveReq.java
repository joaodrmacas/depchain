package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

// Approve request to grant spending allowance
public class ApproveReq extends ClientReq {
    private int allowee; // this guy is allowed to spend by the sender of the request
    private BigInteger amount; // amount to allow

    public ApproveReq(int senderId, Long count, int allowee, BigInteger amount) {
        super(senderId, count);
        this.allowee = allowee;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.APPROVE;
    }

    public int getAllowee() {
        return allowee;
    }

    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return String.format("ApproveReq(senderId(allower)=%d, allowee=%d, amount=%.2f, count=%d)",
                senderId, allowee, amount, count);
    }
}