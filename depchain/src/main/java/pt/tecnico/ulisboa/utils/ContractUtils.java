package pt.tecnico.ulisboa.utils;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.SimpleAccount;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

import pt.tecnico.ulisboa.contracts.Contract;
import pt.tecnico.ulisboa.server.Block;
import pt.tecnico.ulisboa.utils.types.Logger;

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
        // Log before returning
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
            ByteArrayOutputStream outputCopy = new ByteArrayOutputStream();
            outputCopy.write(output.toByteArray());

            String[] lines = outputCopy.toString().split("\\r?\\n");
            // Check if the last line is a JSON object
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

    public static Account getAccountFromAddress(SimpleWorld world, Address address) {
        Account account = world.get(address);
        if (account == null) {
            throw new RuntimeException("Account not found for address: " + address.toHexString());
        }
        return account;
    }
}
