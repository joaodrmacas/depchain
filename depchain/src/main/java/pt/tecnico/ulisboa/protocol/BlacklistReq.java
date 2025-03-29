package pt.tecnico.ulisboa.protocol;

// Blacklist request
public class BlacklistReq extends ClientReq {
    private int toBlacklist;
    private boolean blacklist; // true to blacklist, false to remove from blacklist

    public BlacklistReq(int senderId, Long count, int addressToBlacklist, boolean blacklist) {
        super(senderId, count);
        this.toBlacklist = addressToBlacklist;
        this.blacklist = blacklist;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.BLACKLIST;
    }

    public int getToBlacklist() {
        return toBlacklist;
    }

    public boolean isToBlacklist() {
        return blacklist;
    }

    @Override
    public String toString() {
        return String.format("BlacklistReq(senderId=%d, addressToBlacklist=%d, blacklist?=%b, count=%d)",
                senderId, toBlacklist, blacklist, count);
    }

}