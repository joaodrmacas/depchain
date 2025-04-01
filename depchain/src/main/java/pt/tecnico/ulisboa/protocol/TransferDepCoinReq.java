package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

import org.hyperledger.besu.datatypes.Address;

public class TransferDepCoinReq extends ClientReq {
    private static final long serialVersionUID = 1L;

    private Address receiver;
    private BigInteger amount;

    public TransferDepCoinReq(int senderId, Long count, Address receiver, BigInteger amount) {
        super(senderId, count);
        this.receiver = receiver;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.CONTRACT_CALL;
    }

    public Address getReceiver() {
        return receiver;
    }

    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "TransferDepCoinReq{" +
                "receiver='" + receiver + '\'' +
                ", amount=" + amount +
                ", senderId=" + senderId +
                ", count=" + count +
                '}';
    }

}
