package pt.tecnico.ulisboa;

import java.util.HashMap;

import org.apache.tuweni.crypto.Hash;
import org.hyperledger.besu.datatypes.Address;

public class Config {
    public final static int BUFFER_SIZE = 4096;
    public final static int LEADER_ID = 0;
    public final static int ALLOWED_FAILURES = 1;
    public final static int RETRANSMISSION_TIME = 200;
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
    public final static int MAX_TX_PER_BLOCK = 5;

    public final static int DEFAULT_CLIENT_PORT = 10010;
    public final static int DEFAULT_SERVER_CLIENT_SOCKETS_PORT = 9090;
    public final static int DEFAULT_SERVER_PORT = 8080;

    public final static String GENESIS_BLOCK_PATH = "genesis_block.json";
    public static final int BLOCK_SIZE = 5; // TODO: change this value?
    public static final int ADMIN_ID = -1;
    public static final String MERGED_CONTRACT_ADDRESS = "0x123123123123";
    public static final HashMap<Integer, Address> CLIENT_ID_2_ADDR = new HashMap<Integer, Address>() {
        {
            put(-1, Address.fromHexString("0x000000000000000000000000000000000000000b"));
            put(0, Address.fromHexString("0x0000000000000000000000000000000000000001"));
            put(1, Address.fromHexString("0x0000000000000000000000000000000000000002"));
            put(2, Address.fromHexString("0x0000000000000000000000000000000000000003"));
            put(3, Address.fromHexString("0x0000000000000000000000000000000000000004"));
            put(4, Address.fromHexString("0x0000000000000000000000000000000000000005"));
            put(5, Address.fromHexString("0x0000000000000000000000000000000000000006"));
            put(6, Address.fromHexString("0x0000000000000000000000000000000000000007"));
            put(7, Address.fromHexString("0x0000000000000000000000000000000000000008"));
            put(8, Address.fromHexString("0x0000000000000000000000000000000000000009"));
            put(9, Address.fromHexString("0x000000000000000000000000000000000000000a"));
        }
    };
}