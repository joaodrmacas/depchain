package pt.tecnico.ulisboa.consensus;

import pt.tecnico.ulisboa.network.AuthenticatedPerfectLink;
import pt.tecnico.ulisboa.utils.Logger;

public class EpochChange {
    private AuthenticatedPerfectLink link;
    private int memberId;
    private int epochNumber;

    public EpochChange(AuthenticatedPerfectLink link, int memberId, int epochNumber) {
        this.link = link;
        this.memberId = memberId;
        this.epochNumber = epochNumber;
        Logger.LOG("Creating epoch change");
    }

    public int start() {
        return epochNumber + 1;
    }
}
