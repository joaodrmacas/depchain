package pt.tecnico.ulisboa;

public class Config {
    public final static int BUFFER_SIZE = 4096;
    public final static int LEADER_ID = 0;
    public final static int ALLOWED_FAILURES = 1;
    public final static int RETRANSMISSION_TIME = 50;
    public final static int MAX_COOLDOWN = 5000000;
    public final static int NUM_MEMBERS = 3 * ALLOWED_FAILURES + 1;
    public final static int QUORUM = 2 * ALLOWED_FAILURES + 1;
    public final static int LINK_TIMEOUT = 1000;
    public final static int WAIT_MSG_TIME = 100;
    public final static int CLIENT_KEYPAIR_SIZE = 4096;
    public final static String DEFAULT_KEYS_DIR = "keys";
    public final static int CLIENT_TIMEOUT_MS = 200;
    public final static int DEFAULT_TIMEOUT = 200;
}