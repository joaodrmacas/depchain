package pt.tecnico.ulisboa.server;

import pt.tecnico.ulisboa.protocol.ClientReq;

public class Transaction {
    private final String executed;

    public Transaction(ClientReq request) {
        this.executed = request.toString();
    }
    public String getExecuted() {
        return executed;
    }
    @Override
    public String toString() {
        return "Transaction{" +
                "executed='" + executed + '\'' +
                '}';
    }
}