package pt.tecnico.ulisboa;

import java.util.List;
import java.util.ArrayList;

import org.hyperledger.besu.evm.fluent.SimpleWorld;
import pt.tecnico.ulisboa.protocol.ClientReq;

import pt.tecnico.ulisboa.utils.CryptoUtils;



public class Block {

    private final String prevHash;
    private String blockHash;
    private List<ClientReq> transactions = new ArrayList<>();
    private int transactionCount = 0;
    private State state;

    public Block(String prevHash) {
        this.prevHash = prevHash;
        this.blockHash = null;
        this.transactions = null;
        this.state = null;
    }

    // constructor for genesis block
    public Block(String blockHash, SimpleWorld state) {
        this.prevHash = null;
        this.blockHash = blockHash;
        this.state = new State(state);
    }

    public String finalizeBlock(SimpleWorld state) {
        //TODO: verificar se faz bem a hash disto
        this.blockHash = CryptoUtils.hashSHA256(transactions.toString().getBytes());
        this.state = new State(state);
        return blockHash;
    }

    public void appendTransaction(ClientReq transaction) {
        if (transactionCount >= Config.MAX_TX_PER_BLOCK){
            throw new IllegalStateException("Block is full");
        }
        transactionCount++;
        transactions.add(transaction);
    }

    public boolean isFull() {
        return transactionCount >= Config.MAX_TX_PER_BLOCK;
    }

    public int getTransactionCount() {
        return transactions.size();
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public List<ClientReq> getTransactions() {
        return transactions;
    }

    public State getState() {
        return state;
    }

}