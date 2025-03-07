package pt.tecnico.ulisboa;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public final static int LEADER_ID = 0;
    public final static int ALLOWED_FAILURES = 1;
    public final static int RETRANSMISSION_TIME = 50;
    public final static int MAX_COOLDOWN = 3000;
    public final static int NUM_PROCESSES = 3*ALLOWED_FAILURES + 1;
    public final static int QUORUM = 2*ALLOWED_FAILURES + 1;
    public final static int LINK_TIMEOUT = 1000;
    public final static int WAIT_MSG_TIME = 100;

    public final static Map<Integer, String> id2addr = new HashMap<>();
    public final static Map<String, Integer> addr2id = new HashMap<>();
    
    static {
        for (int i = 0; i < NUM_PROCESSES; i++) {
            id2addr.put(i, "127.0.0.1:" + String.valueOf(8080+i));
            addr2id.put("127.0.0.1:" + String.valueOf(8080+i), i);
        }
    }
}