package pt.tecnico.ulisboa;

import java.math.BigInteger;
import java.util.HashMap;

import org.hyperledger.besu.datatypes.Address;

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
    public final static int MAX_TX_PER_BLOCK = 5;

    public final static int DEFAULT_CLIENT_PORT = 10010;
    public final static int DEFAULT_SERVER_CLIENT_SOCKETS_PORT = 9090;
    public final static int DEFAULT_SERVER_PORT = 8080;

    public final static String GENESIS_BLOCK_PATH = "genesis_block.json";
    public final static String BLOCK_DIRPATH = "blocks";
    public static final int BLOCK_SIZE = 5; // TODO: change this value?
    public static final int ADMIN_ID = -1;
    public static final BigInteger DEPCOIN_PER_IST = BigInteger.valueOf(23);
    public static final HashMap<Integer, String> CLIENT_ID_2_ADDR = new HashMap<Integer, String>() {
        {
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
            put("MergedContract", "0x123123123123");
        }
    };

    public static final HashMap<String, HashMap<String, String>> CONTRACT_METHOD_SIGNATURES = new HashMap<String, HashMap<String, String>>() {
        {
            HashMap<String, String> mergedContractMethods = new HashMap<>();
            mergedContractMethods.put("addToBlacklist", ContractUtils.getFunctionSelector("addToBlacklist(address)"));
            mergedContractMethods.put("removeFromBlacklist",
                    ContractUtils.getFunctionSelector("removeFromBlacklist(address)"));
            mergedContractMethods.put("isBlacklisted", ContractUtils.getFunctionSelector("isBlacklisted(address)"));
            mergedContractMethods.put("transfer", ContractUtils.getFunctionSelector("transfer(address,uint256)"));
            mergedContractMethods.put("transferFrom",
                    ContractUtils.getFunctionSelector("transferFrom(address,address,uint256)"));
            mergedContractMethods.put("approve", ContractUtils.getFunctionSelector("approve(address,uint256)"));
            mergedContractMethods.put("balanceOf", ContractUtils.getFunctionSelector("balanceOf(address)"));
            mergedContractMethods.put("allowance", ContractUtils.getFunctionSelector("allowance(address,address)"));
            mergedContractMethods.put("buy", ContractUtils.getFunctionSelector("buy()"));
            put("MergedContract", mergedContractMethods);
        }
    };
}