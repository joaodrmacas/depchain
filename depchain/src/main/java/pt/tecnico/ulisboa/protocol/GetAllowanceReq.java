package pt.tecnico.ulisboa.protocol;

// Get spending allowance between two accounts
public class GetAllowanceReq extends ClientReq {
    private int allower; // account allower
    private int allowee; // account allowed to spend

    public GetAllowanceReq(Integer id, int count, int allower, int allowee) {
        super(id, count);
        this.allower = allower;
        this.allowee = allowee;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.GET_ALLOWANCE;
    }

    public int getAllower() {
        return allower;
    }

    public int getAllowee() {
        return allowee;
    }

    @Override
    public String toString() {
        return String.format("GetAllowanceReq(id=%d, allower=%d, allowee=%d, count=%d)",
                id, allower, allowee, count);
    }
}