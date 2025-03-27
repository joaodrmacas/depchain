package pt.tecnico.ulisboa.protocol;

// Blacklist request
public class BlacklistReq extends ClientReq {
    private String addressToBlacklist;
    private boolean blacklist; // true to blacklist, false to remove from blacklist

    public BlacklistReq(Integer id, long count, String addressToBlacklist, boolean blacklist) {
        super(id, count);
        this.addressToBlacklist = addressToBlacklist;
        this.blacklist = blacklist;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.BLACKLIST;
    }

    public String getAddressToBlacklist() {
        return addressToBlacklist;
    }

    public boolean isBlacklist() {
        return blacklist;
    }

    @Override
    public String toString() {
        return String.format("BlacklistReq(id=%d, address=%s, blacklist=%b, count=%d)",
                id, addressToBlacklist, blacklist, count);
    }
}