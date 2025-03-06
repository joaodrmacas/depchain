package pt.tecnico.ulisboa;

public class Config {
    public final static int LEADER_ID = 0;
    public final static int ALLOWED_FAILURES = 1;
    public final static int RETRANSMISSION_TIME = 50;
    public final static int MAX_COOLDOWN = 3000;
    public final static int NUM_PROCESSES = 3*ALLOWED_FAILURES + 1;
    public final static int QUORUM = 2*ALLOWED_FAILURES + 1;
}
