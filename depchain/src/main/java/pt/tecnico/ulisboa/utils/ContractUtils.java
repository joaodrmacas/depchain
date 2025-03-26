package pt.tecnico.ulisboa.utils;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import org.hyperledger.besu.datatypes.Address;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.Hash;

import pt.tecnico.ulisboa.utils.types.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ContractUtils {
    
    public static String padAddressTo256Bit(String hexAddress) {
        return String.format("%064x", new BigInteger(hexAddress.substring(2), 16));
    }

    public static String padBigIntegerTo256Bit(BigInteger value) {
        return String.format("%064x", value);
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

    //TODO: test this shit
    public static boolean extractBooleanFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        try {
            String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
            
            // Ensure we have at least one line of JSON output
            if (lines.length == 0) {
                Logger.LOG("No JSON output found");
                return false;
            }
            
            // Parse the last line of JSON
            JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
            
            // Validate presence of required JSON fields
            if (!jsonObject.has("memory") || !jsonObject.has("stack")) {
                Logger.LOG("Missing memory or stack in JSON output");
                return false;
            }
            
            String memory = jsonObject.get("memory").getAsString();
            JsonArray stack = jsonObject.get("stack").getAsJsonArray();
            
            // Validate stack has sufficient elements
            if (stack.size() < 2) {
                Logger.LOG("Insufficient stack elements");
                return false;
            }
            
            // Parse offset and size from stack
            int offset, size;
            try {
                offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
                size = Integer.decode(stack.get(stack.size() - 2).getAsString());
            } catch (NumberFormatException e) {
                Logger.LOG("Failed to parse stack elements: " + e.getMessage());
                return false;
            }
            
            // Validate memory substring access
            int memoryStartIndex = 2 + offset * 2;
            int memoryEndIndex = memoryStartIndex + size * 2;
            
            if (memoryStartIndex < 0 || memoryEndIndex > memory.length()) {
                Logger.LOG("Invalid memory substring indices");
                return false;
            }
            
            // Extract return data
            String returnData = memory.substring(memoryStartIndex, memoryEndIndex);
            Logger.LOG("RETURN DATA: " + returnData);
            
            // In Solidity, booleans are typically the least significant byte (0x00 for false, 0x01 for true)
            // Trim any leading zeros and check the last byte
            returnData = returnData.replaceFirst("^0+", "");
            
            // Return true if last byte is non-zero
            return !returnData.equals("00") && !returnData.isEmpty();
            
        } catch (Exception e) {
            Logger.LOG("Error extracting boolean: " + e.getMessage());
            return false;
        }
    }
    
    //TODO: test this shit
    public static String extractStringFromReturnData(ByteArrayOutputStream byteArrayOutputStream) {
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
    
        String memory = jsonObject.get("memory").getAsString();
    
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
    
        // First word (32 bytes) contains the length of the string
        int stringLength = Integer.decode("0x" + memory.substring(2 + offset * 2, 2 + offset * 2 + 64));
        
        // Actual string data starts after the length word
        String stringHex = memory.substring(2 + offset * 2 + 64, 2 + offset * 2 + 64 + stringLength * 2);
        
        // Convert hex to string
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < stringHex.length(); i += 2) {
            String str = stringHex.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        
        return output.toString();
    }
}
