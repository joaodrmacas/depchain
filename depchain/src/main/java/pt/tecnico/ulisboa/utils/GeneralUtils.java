package pt.tecnico.ulisboa.utils;

import java.util.HashMap;
import java.util.Map;

import pt.tecnico.ulisboa.Config;

public class GeneralUtils {
    public final static Map<Integer, String> id2addr = new HashMap<>();
    public final static Map<String, Integer> addr2id = new HashMap<>();

    static {
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            id2addr.put(i, "127.0.0.1:" + String.valueOf(8080 + i));
            addr2id.put("127.0.0.1:" + String.valueOf(8080 + i), i);
        }
    }

}
