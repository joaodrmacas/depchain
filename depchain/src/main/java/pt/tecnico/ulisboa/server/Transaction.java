package pt.tecnico.ulisboa.server;

import org.hyperledger.besu.datatypes.Address;

import com.google.gson.JsonObject;

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

    public JsonObject toJson() {
        JsonObject txJson = new JsonObject();
        txJson.addProperty("sender", sender.toString());
        txJson.addProperty("receiver", receiver.toString());
        txJson.addProperty("amount", amount);
        txJson.addProperty("functionSignature", functionSignature);
        for (int i = 0; i < args.length; i++) {
            txJson.addProperty("arg" + i, args[i]);
        }
        return txJson;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "executed='" + executed + '\'' +
                '}';
    }
}