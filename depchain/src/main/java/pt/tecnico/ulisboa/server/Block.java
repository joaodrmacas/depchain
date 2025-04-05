package pt.tecnico.ulisboa.server;

import java.util.List;
import java.io.Serializable;
import java.util.ArrayList;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.types.Consensable;

public class Block implements Consensable, Serializable {

    private static final long serialVersionUID = 1L;
    private final int maxTxPerBlock = Config.MAX_TX_PER_BLOCK;

    private Integer blockId;
    private final String prevHash;
    private final String blockHash;
    private final List<ClientReq> transactions;

    // constructor to load a already existing block
    public Block(String prevHash, Integer blockId, String blockHash, List<ClientReq> transactions) {
        this.blockId = blockId;
        this.prevHash = prevHash;
        this.blockHash = blockHash;
        this.transactions = transactions;
    }

    // constructor for genesis block and for server to create an empty block.
    public Block() {
        this.blockId = 0;
        this.prevHash = null;
        this.transactions = new ArrayList<>();
        this.blockHash = computeBlockHash();
    }

    // constructor for genesis block and for server to create an empty block.
    public Block(String prevHash, int blockId, List<ClientReq> txs) {
        this.blockId = blockId;
        this.prevHash = prevHash;
        this.transactions = new ArrayList<>(txs);
        this.blockHash = computeBlockHash();
    }

    public String computeBlockHash() {
        StringBuilder blockData = new StringBuilder();
        blockData.append(prevHash != null ? prevHash : "");
        for (ClientReq tx : transactions) {
            blockData.append(tx.toString());
        }
        return CryptoUtils.hashSHA256(blockData.toString().getBytes());
    }

    public void appendTransaction(ClientReq transaction) {
        if (transactions.size() >= maxTxPerBlock) {
            throw new IllegalStateException("Block is full");
        }
        transactions.add(transaction);
    }

    public boolean isFull() {
        return transactions.size() >= maxTxPerBlock;
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

    public List<ClientReq> getTransactions() {
        return transactions;
    }

    public void setId(Integer blockId) {
        this.blockId = blockId;
    }


    public void printBlock() {
        System.out.println("┌───────────────────────────────────────────────────────────────┐");
        System.out.println("│ Block ID: " + blockId + "                                    │");
        System.out.println("│ Previous Hash: " + prevHash + "                          │");
        System.out.println("│ Block Hash: " + blockHash + "                             │");
        System.out.println("│ Transactions:                                             │");
        for (ClientReq tx : transactions) {
            System.out.println("│ " + tx.toString() + " │");
        }
        System.out.println("└───────────────────────────────────────────────────────────────┘");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Block other = (Block) obj;
        return this.blockHash.equals(other.blockHash);
    }

    @Override
    public int hashCode() {
        return blockHash.hashCode();
    }
}