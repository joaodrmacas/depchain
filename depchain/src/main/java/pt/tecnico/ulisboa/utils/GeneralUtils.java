package pt.tecnico.ulisboa.utils;

import java.util.HashMap;
import java.util.Map;

import pt.tecnico.ulisboa.Config;

public class GeneralUtils {
    public final static Map<Integer, String> id2ClientAddr = new HashMap<>();
    public final static Map<Integer, String> id2ServerAddr = new HashMap<>();
    public final static Map<String, Integer> addr2Id = new HashMap<>();

    static {
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            id2ServerAddr.put(i, String.valueOf(8080 + i));
            id2ClientAddr.put(i, String.valueOf(10100 + i));
            addr2Id.put(String.valueOf(8080 + i), i);
            addr2Id.put(String.valueOf(10100 + i), i);
        }

    }

}
