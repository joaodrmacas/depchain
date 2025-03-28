package pt.tecnico.ulisboa.utils;

import java.util.HashMap;
import java.util.Map;
import java.io.FileReader;
import java.io.IOException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import pt.tecnico.ulisboa.Config;

public class GeneralUtils {
    public final static Map<Integer, String> id2Addr = new HashMap<>();
    public final static Map<Integer, Integer> id2ClientPort = new HashMap<>();
    public final static Map<Integer, Integer> id2ServerPort = new HashMap<>();

    static {
        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            id2Addr.put(i, "127.0.0.1");
            id2ClientPort.put(i, Config.DEFAULT_SERVER_CLIENT_SOCKETS_PORT + i);
            id2ServerPort.put(i, Config.DEFAULT_SERVER_PORT + i);
        }
    }    

}
