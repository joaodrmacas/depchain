package pt.tecnico.ulisboa.server;

import java.util.List;
import java.util.ArrayList;

import org.hyperledger.besu.evm.fluent.SimpleWorld;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.server.Transaction;
import pt.tecnico.ulisboa.utils.CryptoUtils;

public class Block {

    private final int maxTxPerBlock = Config.MAX_TX_PER_BLOCK;

    private final Integer blockId;
    private final String prevHash;
    private String blockHash;
    private List<Transaction> transactions = new ArrayList<>();
    private int transactionCount = 0;

    // constructor to load a already existing block
    public Block(String prevHash, Integer blockId, String blockHash, List<Transaction> transactions) {
        this.blockId = blockId;
        this.prevHash = prevHash;
        this.blockHash = blockHash;
        this.transactions = transactions;
    }

    // constructor for a new block
    public Block(Integer blockId, String prevHash) {
        this.blockId = blockId;
        this.prevHash = prevHash;
        this.blockHash = null;
        this.transactions = null;
    }

    // constructor for genesis block
    public Block() {
        this.blockId = 0;
        this.prevHash = null;
        this.blockHash = CryptoUtils.hashSHA256("".getBytes());
    }

    public void finalizeBlock() {
        StringBuilder blockData = new StringBuilder();
        blockData.append(prevHash != null ? prevHash : "");
        for (Transaction tx : transactions) {
            blockData.append(tx.toString());
        }
        this.blockHash = CryptoUtils.hashSHA256(blockData.toString().getBytes());
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

    public String getHash() {
        return blockHash;
    }

    public Integer getId() {
        return blockId;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

}