package pt.tecnico.ulisboa.server;

import java.util.List;
import java.util.ArrayList;

import org.hyperledger.besu.evm.fluent.SimpleWorld;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.server.Transaction;
import pt.tecnico.ulisboa.utils.CryptoUtils;

public class Block {

    private final String prevHash;
    private String blockHash;
    private List<Transaction> transactions = new ArrayList<>();
    private int transactionCount = 0;
    private SimpleWorld state;
    private int maxTxPerBlock = Config.MAX_TX_PER_BLOCK;

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
        this.state = state;
    }

    public String finalizeBlock(SimpleWorld state) {
        // TODO: massas ve se achas que isto ta bem
        StringBuilder blockData = new StringBuilder();
        blockData.append(prevHash != null ? prevHash : ""); // Include previous block hash
        for (Transaction tx : transactions) {
            blockData.append(tx.toString()); // Include all transactions
        }
        blockData.append(state != null ? state.toString() : ""); // Include state if available

        this.blockHash = CryptoUtils.hashSHA256(blockData.toString().getBytes());
        this.state = state;
        return blockHash;
    }

    public void appendTransaction(Transaction transaction) {
        if (transactionCount >= maxTxPerBlock) {
            throw new IllegalStateException("Block is full");
        }
        transactionCount++;
        transactions.add(transaction);
    }

    public boolean isFull() {
        return transactionCount >= maxTxPerBlock;
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

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public SimpleWorld getState() {
        return state;
    }

}