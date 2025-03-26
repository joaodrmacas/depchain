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
        String[] lines = byteArrayOutputStream.toString().split("\\r?\\n");
        JsonObject jsonObject = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
    
        String memory = jsonObject.get("memory").getAsString();
    
        JsonArray stack = jsonObject.get("stack").getAsJsonArray();
        int offset = Integer.decode(stack.get(stack.size() - 1).getAsString());
        int size = Integer.decode(stack.get(stack.size() - 2).getAsString());
    
        String returnData = memory.substring(2 + offset * 2, 2 + offset * 2 + size * 2);
        // In Solidity, booleans are stored as the least significant byte (0x00 for false, 0x01 for true)
        return !returnData.endsWith("00");
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
