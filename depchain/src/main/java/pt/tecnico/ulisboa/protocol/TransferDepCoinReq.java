package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

import org.hyperledger.besu.datatypes.Address;

public class TransferDepCoinReq extends ClientReq {
    private static final long serialVersionUID = 1L;

    private String receiverAddr;
    private BigInteger amount;

    public TransferDepCoinReq(int senderId, Long count, String receiver, BigInteger amount) {
        super(senderId, count);
        this.receiverAddr = receiver;
        this.amount = amount;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.TRANSFER_DEP_COIN;
    }

    public Address getReceiver() {
        return Address.fromHexString(receiverAddr);
    }

    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "TransferDepCoinReq{" +
                "receiver='" + receiverAddr + '\'' +
                ", amount=" + amount +
                ", senderId=" + senderId +
                ", count=" + count +
                '}';
    }

    @Override
    public boolean needsConsensus() {
        return true;
    }

}
