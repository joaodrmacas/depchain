package pt.tecnico.ulisboa.protocol;

// Check balance request
public class CheckBalanceReq extends ClientReq {
    private int account; // account to check balance for

    public CheckBalanceReq(Integer id, int count, int account) {
        super(id, count);
        this.account = account;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.CHECK_BALANCE;
    }

    public int getAccount() {
        return account;
    }

    @Override
    public String toString() {
        return String.format("CheckBalanceReq(id=%d, account=%d, count=%d)",
                id, account, count);
    }
}