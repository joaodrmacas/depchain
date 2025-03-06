package pt.tecnico.ulisboa.consensus;

import pt.tecnico.ulisboa.network.AuthenticatedPerfectLink;

public class EpochChange {
    private AuthenticatedPerfectLink link;
    private int processId;
    private int epochNumber;
    private int index;

    public EpochChange(int index, AuthenticatedPerfectLink link, int processId, int epochNumber) {
        this.link = link;
        this.processId = processId;
        this.epochNumber = epochNumber;
        this.index = index;
        System.out.println("Creating epoch change");
    }

    public int start() {
        return epochNumber + 1;
    }
}
