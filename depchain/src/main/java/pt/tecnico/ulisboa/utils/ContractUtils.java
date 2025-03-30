package pt.tecnico.ulisboa.utils;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.SimpleWorld;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.tecnico.ulisboa.utils.types.Logger;
import pt.tecnico.ulisboa.server.Account;
import pt.tecnico.ulisboa.server.Block;
import pt.tecnico.ulisboa.server.Transaction;
import pt.tecnico.ulisboa.contracts.Contract;


public class ContractUtils {

    public static String padBigIntegerTo256Bit(BigInteger value) {
        return String.format("%064x", value);
    }

    public static String padAddressTo256Bit(Address addr) {
        String addrHex = addr.toHexString().substring(2); // Remove '0x'
        return "0".repeat(64 - addrHex.length()) + addrHex;
    }

    public static String padHexStringTo256Bit(String hexString) {
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }

        int length = hexString.length();
        int targetLength = 64;

        if (length >= targetLength) {
            return hexString.substring(0, targetLength);
        }

        return "0".repeat(targetLength - length) +
                hexString;
    }

    public static String convertIntegerToHex256Bit(int number) {
        BigInteger bigInt = BigInteger.valueOf(number);
        return String.format("%064x", bigInt);
    }

    public static Address generateAddressFromId(int clientId) {
        try {
            // Convert int to byte array (4 bytes)
            byte[] idBytes = ByteBuffer.allocate(4).putInt(clientId).array();

            // Compute Keccak-256 hash
            MessageDigest digest = MessageDigest.getInstance("KECCAK-256");
            byte[] hash = digest.digest(idBytes);

            // Extract last 20 bytes (Ethereum address)
            byte[] addressBytes = new byte[20];
            System.arraycopy(hash, hash.length - 20, addressBytes, 0, 20);

            // Create Besu Address object using its constructor
            return Address.fromHexString("0x" + bytesToHex(addressBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Keccak-256 algorithm not available", e);
        }
    }

    // Utility method to convert byte array to hex string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String getFunctionSelector(String signature) {
        byte[] hash = Hash.sha3(signature.getBytes(StandardCharsets.UTF_8));
        return Numeric.toHexString(Arrays.copyOfRange(hash, 0, 4)); // First 4 bytes
    }

    public static int extractIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return Integer.decode("0x" + returnData);
    }

    public static BigInteger extractBigIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);

        // Convert to BigInteger to handle large hex values
        return new BigInteger(returnData, 16);
    }

    public static boolean extractBooleanFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString().substring(2); // Remove '0x'

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        // Extract the relevant portion of memory
        String returnData = memory.substring(offset * 2, offset * 2 + size * 2);

        // Remove leading zeros
        returnData = returnData.replaceFirst("^0+", "");

        // Convert to boolean (any non-zero value is true)
        return !returnData.equals("0") && !returnData.isEmpty();
    }

    public static void checkForExecutionErrors(ByteArrayOutputStream output) {
        try {
            // Create a copy of the output stream to avoid consuming it
            // TODO: acho que nao precisa de ser copiado
            ByteArrayOutputStream outputCopy = new ByteArrayOutputStream();
            outputCopy.write(output.toByteArray());

            String[] lines = outputCopy.toString().split("\\r?\\n");
            if (lines.length > 0) {
                JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
                if (jsonObject.has("error")) {
                    String errorMessage = jsonObject.get("error").getAsString();
                    throw new RuntimeException("Execution error: " + errorMessage);
                }
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            Logger.LOG("Error checking execution output: " + e.getMessage());
        }
    }

    public static String extractHexStringFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString().substring(2); // Remove '0x'
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        // Extract the relevant portion of memory
        String returnData = memory.substring(offset * 2, offset * 2 + size * 2);

        // Remove leading zeros
        returnData = returnData.replaceFirst("^0+", "");

        return returnData;
    }

    public static void worldToJson(SimpleWorld world, Block block, Map<Address, Account> clientAccounts, Map<Address, Contract> contracts) {
        
        JsonObject rootObj = new JsonObject();
        
        rootObj.addProperty("block_hash", block.getBlockHash());
        if (block.getPrevHash() != null) {
            rootObj.addProperty("previous_block_hash", block.getPrevHash());
        } else {
            rootObj.add("previous_block_hash", null);
        }

        JsonArray txArray = new JsonArray();
        for (Transaction tx : block.getTransactions()) {
            txArray.add(tx.toString());
        }
        rootObj.add("transactions", txArray);

        JsonObject stateObj = new JsonObject();
        for (Map.Entry<Address, Account> entry : clientAccounts.entrySet()) {
            Address addr = entry.getKey();
            Account account = entry.getValue();
            JsonObject accountObj = new JsonObject();
            accountObj.addProperty("balance", account.getBalance());
            stateObj.add(addr.toHexString(), accountObj);
        }

        for (Map.Entry<Address, Contract> entry : contracts.entrySet()) {
            Address addr = entry.getKey();
            Contract contract = entry.getValue();
            MutableAccount contractAccount = (MutableAccount) world.get(addr);
            JsonObject contractObj = new JsonObject();
            contractObj.addProperty("balance", contractAccount.getBalance().getValue()); //TODO: hardcoded value
            contractObj.addProperty("deploy_code", contract.getDeployCode());  
            contractObj.addProperty("runtime_code", contract.getRuntimeCode());
            
            //Get dump
            JsonObject dump = dumpContractStorage(world, addr, clientAccounts.keySet().stream().toList());
            contractObj.addProperty("storage", dump.toString());
            stateObj.add(addr.toHexString(), contractObj);
        }




    }

    //TODO: confirmar isto
    public static JsonObject dumpContractStorage(SimpleWorld world, Address contractAddress, List<Address> clientAddresses) {
        // Get the contract account
        MutableAccount contractAccount = (MutableAccount) world.get(contractAddress);
        
        JsonObject result = new JsonObject();
        result.addProperty("contract", contractAddress.toHexString());
        
        // Regular slots
        JsonObject regularSlots = new JsonObject();
        for (int i = 0; i < 100; i++) {
            UInt256 slot = UInt256.valueOf(i);
            UInt256 value = contractAccount.getStorageValue(slot);
            
            if (!value.isZero()) {
                regularSlots.addProperty(String.valueOf(i), value.toHexString());
            }
        }
        result.add("regularSlots", regularSlots);
        
        // Mapping slots
        JsonObject mappingSlots = new JsonObject();
        String mappingSlot = "1"; // Base slot of the mapping
        
        for (Address addr : clientAddresses) {
            // Calculate mapping slot: keccak256(key + mappingSlot)
            String paddedAddr = padHexStringTo256Bit(addr.toHexString());
            String paddedSlot = padHexStringTo256Bit(mappingSlot);
            String computedSlot = Numeric.toHexStringNoPrefix(
                Hash.sha3(Numeric.hexStringToByteArray(paddedAddr + paddedSlot))
            );
            
            UInt256 mappingKeySlot = UInt256.fromHexString(computedSlot);
            UInt256 value = contractAccount.getStorageValue(mappingKeySlot);
            
            if (!value.isZero()) {
                JsonObject mappingEntry = new JsonObject();
                mappingEntry.addProperty("slot", computedSlot);
                mappingEntry.addProperty("value", value.toHexString());
                mappingSlots.add(addr.toHexString(), mappingEntry);
            }
        }
        result.add("mappingSlots", mappingSlots);
        
        // Add contract metadata
        JsonObject metadata = new JsonObject();
        metadata.addProperty("codeHash", contractAccount.getCodeHash().toHexString());
        metadata.addProperty("balance", contractAccount.getBalance().toHexString());
        metadata.addProperty("nonce", contractAccount.getNonce());
        result.add("metadata", metadata);
        
        return result;
    }

    // public static String extractStringFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
    //     String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
    //     JsonObject jsonObject = JsonParser.parseString(lines[lines.length-1]).getAsJsonObject();

    //     String memory = jsonObject.get("memory").getAsString();

    //     JsonArray stack = jsonObject.get("stack").getAsJsonArray();
    //     int offset = Integer.decode(stack.get(stack.size()-1).getAsString());
    //     int size = Integer.decode(stack.get(stack.size()-2).getAsString());

    //     String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);

    //     int stringOffset = Integer.decode("0x"+returnData.substring(0, 32 * 2));
    //     int stringLength = Integer.decode("0x"+returnData.substring(stringOffset * 2, stringOffset * 2 + 32 * 2));
    //     String hexString = returnData.substring(stringOffset * 2 + 32 * 2, stringOffset * 2 + 32 * 2 + stringLength * 2);

    //     return new String(hexStringToByteArray(hexString), StandardCharsets.UTF_8);
    // }
}
