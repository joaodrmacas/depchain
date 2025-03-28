package pt.tecnico.ulisboa.protocol;

// Check if an toCheck is blacklisted
public class IsBlacklistedReq extends ClientReq {
    private int sender; // who sends the request
    private int toCheck; // client to check blacklist status

    public IsBlacklistedReq(Integer id, int count, int toCheck) {
        super(id, count);
        this.toCheck = toCheck;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.IS_BLACKLISTED;
    }

    public int getSender() {
        return sender;
    }

    public int getToCheck() {
        return toCheck;
    }

    @Override
    public String toString() {
        return String.format("IsBlacklistedReq(sender=%d, id=%d, toCheck=%d, count=%d)",
                sender, id, toCheck, count);
    }
}