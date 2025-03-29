// TODO: is sender the allower or allowee

package pt.tecnico.ulisboa.protocol;

// Get spending allowance between two accounts
public class GetAllowanceReq extends ClientReq {
    private int allower; // how much does this account allow the sender of the request to spend

    public GetAllowanceReq(int senderId, Long count, int allowee) {
        super(senderId, count);
        this.allower = allowee;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.GET_ALLOWANCE;
    }

    public int getAllower() {
        return allower;
    }

    @Override
    public String toString() {
        return String.format("GetAllowanceReq(senderId(allowee)=%d, allower=%d, count=%d)",
                senderId, allower, count);
    }
}