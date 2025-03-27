package pt.tecnico.ulisboa.utils;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import org.hyperledger.besu.datatypes.Address;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.Hash;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    
    public static Address generateContractAddress(Address deployerAddress) {
        return Address.fromHexString(
            org.web3j.crypto.Hash.sha3String(
                deployerAddress.toHexString() + 
                System.currentTimeMillis()
            ).substring(0, 42)
        );
    }

    public static String getFunctionSelector(String signature){
        Bytes hash = Hash.keccak256(Bytes.of(signature.getBytes()));
        return hash.slice(0, 4).toHexString(); // First 4 bytes
    }

    public static int extractIntegerFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();

        String memory = jsonObject.get("memory").getAsString();

        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());

        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        return Integer.decode("0x"+returnData);
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

    public static String extractStringFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
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
}
