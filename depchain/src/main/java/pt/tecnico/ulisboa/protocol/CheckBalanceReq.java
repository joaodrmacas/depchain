package pt.tecnico.ulisboa.protocol;

// Check balance request
public class CheckBalanceReq extends ClientReq {

    public CheckBalanceReq(int senderId, Long count) {
        super(senderId, count);
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.CHECK_BALANCE;
    }

    @Override
    public String toString() {
        return String.format("CheckBalanceReq(senderId(account)=%d, count=%d)",
                senderId, count);
    }
}