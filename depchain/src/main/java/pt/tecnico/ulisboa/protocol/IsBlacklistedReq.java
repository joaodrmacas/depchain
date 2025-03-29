package pt.tecnico.ulisboa.protocol;

// Check if an toCheck is blacklisted
public class IsBlacklistedReq extends ClientReq {
    private int toCheck; // client to check blacklist status

    public IsBlacklistedReq(int senderId, Long count, int toCheck) {
        super(senderId, count);
        this.toCheck = toCheck;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.IS_BLACKLISTED;
    }

    public int getToCheck() {
        return toCheck;
    }

    @Override
    public String toString() {
        return String.format("IsBlacklistedReq(senderId=%d, toCheck=%d, count=%d)",
                senderId, toCheck, count);
    }
}