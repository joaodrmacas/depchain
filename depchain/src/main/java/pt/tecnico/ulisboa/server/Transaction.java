package pt.tecnico.ulisboa.server;

import org.hyperledger.besu.datatypes.Address;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

import pt.tecnico.ulisboa.protocol.ClientReq;

//TODO: put here the sender, the receiver, the amount of depcoin, function signature and args
public class Transaction {
    private final String executed;
    private Address sender;
    private Address receiver;
    private int amount;
    private String functionSignature;
    private String[] args;

    public Transaction(ClientReq request) {
        this.executed = request.toString();
    }
    public String getExecuted() {
        return executed;
    }

    // public JsonObject toJson() {
    //     JsonObject txJson = new JsonObject();
    //     txJson.addProperty("sender", sender.toString());
    //     txJson.addProperty("receiver", receiver.toString());
    //     txJson.addProperty("amount", amount);
    //     if (functionSignature != null) {
    //         txJson.addProperty("functionSignature", functionSignature);
    //     }
    //     if (args != null) {
    //         JsonObject argsJson = new JsonObject();
    //         for (int i = 0; i < args.length; i++) {
    //             argsJson.addProperty("arg" + i, args[i]);
    //         }
    //         txJson.add("args", argsJson);
    //     }
    //     return txJson;
    // }

    public JsonObject toJson() {
        return new Gson().toJsonTree(this).getAsJsonObject();
    }

    //TODO: Test if it works
    public static Transaction fromJson(JsonObject json) {
        return new Gson().fromJson(json, Transaction.class);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}