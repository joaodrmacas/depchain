package pt.tecnico.ulisboa.utils;

import java.util.HashMap;
import java.util.Map;

import pt.tecnico.ulisboa.Config;

public class GeneralUtils {
    public final static Map<Integer, String> id2Addr = new HashMap<>();
    public final static Map<Integer, Integer> id2ClientPort = new HashMap<>();
    public final static Map<Integer, Integer> id2ServerPort = new HashMap<>();

    static {
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            id2Addr.put(i, "localhost");
            id2ClientPort.put(i, 9090 + i);
            id2ServerPort.put(i, 8080 + i);
        }

    }

}
