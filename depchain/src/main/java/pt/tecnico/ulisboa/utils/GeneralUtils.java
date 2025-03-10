package pt.tecnico.ulisboa.utils;

import java.util.HashMap;
import java.util.Map;

import pt.tecnico.ulisboa.Config;

public class GeneralUtils {
    public final static Map<Integer, String> serversId2Addr = new HashMap<>();
    public final static Map<String, Integer> serversAddr2Id = new HashMap<>();
    public final static Map<Integer, String> clientsId2Addr = new HashMap<>();
    public final static Map<String, Integer> clientsAddr2Id = new HashMap<>();

    static {
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            serversId2Addr.put(i, "127.0.0.1:" + String.valueOf(8080 + i));
            serversAddr2Id.put("127.0.0.1:" + String.valueOf(8080 + i), i);
            clientsId2Addr.put(i, "127.0.0.1:" + String.valueOf(9090 + i));
            clientsAddr2Id.put("127.0.0.1:" + String.valueOf(9090 + i), i);
        }
    }

}
