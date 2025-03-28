package pt.tecnico.ulisboa.protocol;

// Blacklist request
public class BlacklistReq extends ClientReq {
    private int sender; // who sends the request
    private int toBlacklist;
    private boolean blacklist; // true to blacklist, false to remove from blacklist

    public BlacklistReq(Integer id, int count, int addressToBlacklist, boolean blacklist) {
        super(id, count);
        this.toBlacklist = addressToBlacklist;
        this.blacklist = blacklist;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.BLACKLIST;
    }

    public int getSender() {
        return sender;
    }

    public int getToBlacklist() {
        return toBlacklist;
    }

    public boolean isToBlacklist() {
        return blacklist;
    }

    @Override
    public String toString() {
        return String.format("BlacklistReq(sender=%d, id=%d, address=%d, blacklist=%b, count=%d)",
                sender, id, toBlacklist, blacklist, count);
    }

}