package pt.tecnico.ulisboa;

public class Config {
    public final static int BUFFER_SIZE = 4096;
    public final static int LEADER_ID = 0;
    public final static int ALLOWED_FAILURES = 1;
    public final static int RETRANSMISSION_TIME = 300;
    public final static int MAX_COOLDOWN = 5000000;
    public final static int NUM_MEMBERS = 3 * ALLOWED_FAILURES + 1;
    public final static int QUORUM = 2 * ALLOWED_FAILURES + 1;
    public final static int CONSENSUS_LINK_TIMEOUT = 2000;
    public final static int NEW_TRANSACTION_TIMEOUT = 2000;
    public final static int WAIT_MSG_TIME = 100;
    public final static int CLIENT_KEYPAIR_SIZE = 4096;
    public final static String DEFAULT_KEYS_DIR = "keys";
    public final static String DEFAULT_LOGS_DIR = "logs";
    public final static int CLIENT_TIMEOUT_MS = 2000;
    public final static int DEFAULT_TIMEOUT = 10000;
    public final static int MAX_FRAGMENT_SIZE = 1024;

    public final static int DEFAULT_CLIENT_PORT = 10010;
    public final static int DEFAULT_SERVER_CLIENT_SOCKETS_PORT = 9090;
    public final static int DEFAULT_SERVER_PORT = 8080;

}