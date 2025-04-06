package pt.tecnico.ulisboa;

import java.math.BigInteger;
import java.util.HashMap;

import pt.tecnico.ulisboa.utils.ContractUtils;

public class Config {
    public final static int BUFFER_SIZE = 4096;
    public final static int LEADER_ID = 0;
    public final static int ALLOWED_FAILURES = 1;
    public final static int RETRANSMISSION_TIME = 300;
    public final static int MAX_COOLDOWN = 5000000;
    public final static int NUM_MEMBERS = 3 * ALLOWED_FAILURES + 1;
    public final static int QUORUM = 2 * ALLOWED_FAILURES + 1;
    public final static int CONSENSUS_LINK_TIMEOUT = 3000;
    public final static int NEW_TRANSACTION_TIMEOUT = 3000;
    public final static int WAIT_MSG_TIME = 100;
    public final static int CLIENT_KEYPAIR_SIZE = 4096;
    public final static String DEFAULT_KEYS_DIR = "keys";
    public final static String DEFAULT_LOGS_DIR = "logs";
    public final static int CLIENT_TIMEOUT_MS = 2000;
    public final static int DEFAULT_TIMEOUT = 10000;
    public final static int MAX_FRAGMENT_SIZE = 1024;
    public final static int TX_PER_BLOCK = 1;

    public final static int DEFAULT_CLIENT_PORT = 10010;
    public final static int DEFAULT_SERVER_CLIENT_SOCKETS_PORT = 9090;
    public final static int DEFAULT_SERVER_PORT = 8080;

    public final static String GENESIS_BLOCK_PATH = "genesis_block.json";
    public final static String BLOCK_DIRPATH = "blocks";
    public static final int ADMIN_ID = -1;
    public static final BigInteger DEPCOIN_PER_IST = BigInteger.valueOf(23000);
    public static final HashMap<Integer, String> CLIENT_ID_2_ADDR = new HashMap<Integer, String>() {
        {
            put(-4, "0x000000000000000000000000000000000000000e");
            put(-3, "0x000000000000000000000000000000000000000d");
            put(-2, "0x000000000000000000000000000000000000000c");
            put(-1, "0x000000000000000000000000000000000000000b");
            put(0, "0x0000000000000000000000000000000000000001");
            put(1, "0x0000000000000000000000000000000000000002");
            put(2, "0x0000000000000000000000000000000000000003");
            put(3, "0x0000000000000000000000000000000000000004");
            put(4, "0x0000000000000000000000000000000000000005");
            put(5, "0x0000000000000000000000000000000000000006");
            put(6, "0x0000000000000000000000000000000000000007");
            put(7, "0x0000000000000000000000000000000000000008");
            put(8, "0x0000000000000000000000000000000000000009");
            put(9, "0x000000000000000000000000000000000000000a");
        }
    };

    public static final HashMap<String, String> CONTRACT_NAME_2_ADDR = new HashMap<String, String>() {
        {
            put("ISTContract", "0x123123123123");
        }
    };

    public static final HashMap<String, HashMap<String, String>> CONTRACT_METHOD_SIGNATURES = new HashMap<String, HashMap<String, String>>() {
        {
            HashMap<String, String> istContractMethods = new HashMap<>();
            istContractMethods.put("addToBlacklist", ContractUtils.getFunctionSelector("addToBlacklist(address)"));
            istContractMethods.put("removeFromBlacklist",
                    ContractUtils.getFunctionSelector("removeFromBlacklist(address)"));
            istContractMethods.put("isBlacklisted", ContractUtils.getFunctionSelector("isBlacklisted(address)"));
            istContractMethods.put("transfer", ContractUtils.getFunctionSelector("transfer(address,uint256)"));
            istContractMethods.put("transferFrom",
                    ContractUtils.getFunctionSelector("transferFrom(address,address,uint256)"));
            istContractMethods.put("approve", ContractUtils.getFunctionSelector("approve(address,uint256)"));
            istContractMethods.put("balanceOf", ContractUtils.getFunctionSelector("balanceOf(address)"));
            istContractMethods.put("allowance", ContractUtils.getFunctionSelector("allowance(address,address)"));
            istContractMethods.put("buy", ContractUtils.getFunctionSelector("buy()"));
            istContractMethods.put("owner", ContractUtils.getFunctionSelector("owner()"));
            put("ISTContract", istContractMethods);
        }
    };
}