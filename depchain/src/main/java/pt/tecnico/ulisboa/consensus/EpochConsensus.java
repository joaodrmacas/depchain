package pt.tecnico.ulisboa.consensus;

import pt.tecnico.ulisboa.Config;

public class BFT {

    public void start() {
        while (true) {
            EpochConsensus epoch = new EpochConsensus();
            epoch.start();
            epoch.sendWrite("value");
            epoch.sendRead();
            epoch.sendAccept("value");
            epoch.endEpoch();
        }
    }

    public void getLeader(int epochNumber) {
        return Config.LEADER_ID;
    }
}


public class EpochConsensus {
    private int epochNumber;

    public EpochConsensus() {
        System.out.println("Creating epoch consensus");
    }

    public void start() {
        System.out.println("Starting epoch");
    }

    public void endEpoch() {
        System.out.println("Ending epoch");
    }

    public void sendWrite(String value) {
        System.out.println("Sending write: " + value);
    }

    public void sendRead() {
        System.out.println("Sending read");
    }

    public void sendAccept(String value) {
        System.out.println("Sending accept: " + value);
    }
}
