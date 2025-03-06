package pt.tecnico.ulisboa.consensus;

import Config;

public claConfig{

    public void start() {
        int epochNumber;
        
        while (true) {
            EpochConsensus epoch = new EpochConsensus();
            if (!epoch.start()) {
                EpochChange epochChange = new EpochChange();
                epochNumber = epochChange.start();
            }
        }
    }

    public void getLeader(int epoc             {
        return Config.        E

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
R_

class EpochConsensus {
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
}public void sendRead() {
            System.out.println("Sending read");
        }

        public void sendAccept(String value) {
            System.out.println("Sending accept: " + value);
        }
    }
}
