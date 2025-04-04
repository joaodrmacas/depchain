package pt.tecnico.ulisboa.protocol;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.math.BigInteger;

public class ClientReqFactory {

    public static ClientReq fromJson(JsonObject json) {
        if (!json.has("reqType")) {
            throw new IllegalArgumentException("JSON does not contain reqType field");
        }

        ClientReq.ClientReqType reqType = ClientReq.ClientReqType.valueOf(json.get("reqType").getAsString());
        
        // Common fields
        int senderId = json.get("senderId").getAsInt();
        Long count = json.get("count").getAsLong();
        
        ClientReq request;
        
        switch (reqType) {
            case BALANCE_OF_DEP_COIN:
                String ofAddr = json.get("ofAddr").getAsString();
                request = new BalanceOfDepCoinReq(senderId, count, ofAddr);
                break;
                
            case TRANSFER_DEP_COIN:
                String receiverAddr = json.get("receiverAddr").getAsString();
                BigInteger amount = new BigInteger(json.get("amount").getAsString());
                request = new TransferDepCoinReq(senderId, count, receiverAddr, amount);
                break;
                
            case CONTRACT_CALL:
                String contractName = json.get("contractName").getAsString();
                String methodName = json.get("methodName").getAsString();
                BigInteger value = json.has("value") ? 
                        new BigInteger(json.get("value").getAsString()) : 
                        BigInteger.ZERO;
                        
                // Parse arguments if present
                String[] args = new String[0];
                if (json.has("argsArray") && json.get("argsArray").isJsonArray()) {
                    JsonArray argsArray = json.getAsJsonArray("argsArray");
                    args = new String[argsArray.size()];
                    for (int i = 0; i < argsArray.size(); i++) {
                        args[i] = argsArray.get(i).getAsString();
                    }
                } else if (json.has("args") && !json.get("args").getAsString().isEmpty()) {
                    args = json.get("args").getAsString().split(", ");
                }
                
                // Create ContractCallReq with appropriate constructor based on whether value is zero
                if (value.equals(BigInteger.ZERO) && args.length == 0) {
                    request = new ContractCallReq(senderId, count, contractName, methodName);
                } else if (value.equals(BigInteger.ZERO)) {
                    request = new ContractCallReq(senderId, count, contractName, methodName, (Object[]) args);
                } else {
                    request = new ContractCallReq(senderId, count, contractName, methodName, value, (Object[]) args);
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unknown reqType: " + reqType);
        }
        
        // Set signature if present
        if (json.has("signature")) {
            request.setSignature(json.get("signature").getAsString());
        }
        
        return request;
    }
}