package pt.tecnico.ulisboa.server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.crypto.sodium.PasswordHash.VerificationResult;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.SimpleAccount;
import org.hyperledger.besu.evm.fluent.SimpleWorld;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.utils.types.Logger;

public class BlockchainPersistenceManager {
    
    private final Gson gson;
    private final String dataDirectory;
    private final String genesisBlockFile;

    public BlockchainPersistenceManager() {
        this.dataDirectory = Config.BLOCK_DIRPATH;
        this.genesisBlockFile = Config.BLOCK_DIRPATH + "/" + Config.GENESIS_BLOCK_PATH;

        // Create directory if it doesn't exist
        File dir = new File(dataDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    //TODO: opinioes needed (tanto para este como para o loadblock)
    //Tenho que passar aqui a blockchain pq so assim altera por referencia...
    //Podemos considerar ter uma class que é block + state que seria um blockchain state
    //ou tmb posso retornar um par idk let me know what u think - Massas
    public SimpleWorld loadGenesisBlock(List<Block> blockchain) throws IOException {
        try (FileReader reader = new FileReader(genesisBlockFile)) {

            JsonObject rootObj = JsonParser.parseReader(reader).getAsJsonObject();
            Block genesisBlock = new Block();
            blockchain.add(genesisBlock);

            SimpleWorld world = worldFromJson(rootObj.getAsJsonObject("state"));
            return world;
            
        } catch (IOException e) {
            throw new IOException("Failed to load genesis block: " + e.getMessage(), e);
        }
    }

    private SimpleWorld loadBlock(int blockId, List<Block> blockchain) throws IOException {
        String blockFileName = String.format("%s/block_%d.json", dataDirectory, blockId);
        try (FileReader reader = new FileReader(blockFileName)) {
            JsonObject rootObj = JsonParser.parseReader(reader).getAsJsonObject();

            // Load the block
            String blockHash = rootObj.get("block_hash").getAsString();
            String prevHash = rootObj.has("previous_block_hash") ? rootObj.get("previous_block_hash").getAsString() : null;
            JsonArray txArray = rootObj.getAsJsonArray("transactions");
            List<Transaction> transactions = new ArrayList<>();
            for (JsonElement txElement : txArray) {
                Transaction tx = Transaction.fromJson(txElement.getAsJsonObject());
                transactions.add(tx);
            }
            blockchain.add(new Block(prevHash, blockId, blockHash, transactions));
            SimpleWorld world = worldFromJson(rootObj.getAsJsonObject("state"));
            return world;
            
        } catch (IOException e) {
            throw new IOException("Failed to load block: " + e.getMessage(), e);
        }
    }

    public void persistBlock(Block block, SimpleWorld world) throws IOException {
        // Create JSON for the block and world state
        JsonObject rootObj = new JsonObject();
        
        // Set block properties
        rootObj.addProperty("block_hash", block.getHash());
        if (block.getPrevHash() != null) {
            rootObj.addProperty("previous_block_hash", block.getPrevHash());
        } else {
            rootObj.add("previous_block_hash", null);
        }
        
        // Add transactions
        JsonArray txArray = new JsonArray();
        for (Transaction tx : block.getTransactions()) {
            txArray.add(tx.toJson());
        }
        rootObj.add("transactions", txArray);
        
        // Add world state
        JsonObject stateObj = worldToJson(world);
        
        rootObj.add("state", stateObj);
        
        // Write to file
        String blockFileName = String.format("%s/block_%d.json", dataDirectory, block.getId());
        try (FileWriter writer = new FileWriter(blockFileName)) {
            gson.toJson(rootObj, writer);
        }
    }

    public boolean verifyBlockchainIntegrity(List<Block> blockchain) {
        if (blockchain == null || blockchain.isEmpty()) {
            Logger.LOG("Blockchain is empty or null");
            return true;
        }
        
        // Check that the first block is the genesis block
        Block genesisBlock = blockchain.get(0);
        if (genesisBlock.getPrevHash() != null) {
            Logger.LOG("Genesis block should not have a previous hash");
            return false;
        }
        
        // Check that blocks are in sequence and reference each other correctly
        for (int i = 1; i < blockchain.size(); i++) {
            Block currentBlock = blockchain.get(i);
            Block previousBlock = blockchain.get(i - 1);
            
            // Check block ID sequence
            if (currentBlock.getId() != i) {
                Logger.LOG("Block ID mismatch: expected " + i + ", got " + currentBlock.getId());
                return false;
            }
            
            // Check previous hash reference
            if (!currentBlock.getPrevHash().equals(previousBlock.getHash())) {
                Logger.LOG("Previous hash mismatch: expected " + previousBlock.getHash() + ", got " + currentBlock.getPrevHash());
                return false;
            }
        }
        
        Logger.LOG("Blockchain integrity verified successfully");
        return true;
    }

    public SimpleWorld loadBlockchain(List<Block> blockchain) throws IOException {
        blockchain.clear();
        
        File dir = new File(dataDirectory);
        
        if (!new File(genesisBlockFile).exists()) {
            throw new IOException("Genesis block file not found: " + genesisBlockFile);
        }
        
        SimpleWorld currentWorld = loadGenesisBlock(blockchain);
        
        // Find all block files and sort them by block number
        File[] blockFiles = dir.listFiles((_, name) -> name.startsWith("block_") && name.endsWith(".json"));
        if (blockFiles != null && blockFiles.length > 0) {
            Arrays.sort(blockFiles, Comparator.comparingInt(f -> {
                String fileName = f.getName();
                return Integer.parseInt(fileName.substring(6, fileName.length() - 5));
            }));
            
            // Load each block in order
            for (File blockFile : blockFiles) {
                String fileName = blockFile.getName();
                int blockId = Integer.parseInt(fileName.substring(6, fileName.length() - 5));
                
                // Skip block_0.json if it exists since we already loaded the genesis block
                if (blockId == 0) {
                    continue;
                }
                
                // Load the block and update the world state
                currentWorld = loadBlock(blockId, blockchain);
            }
        }
        
        return currentWorld;
    }

    private SimpleWorld worldFromJson(JsonObject jsonObject) {
        SimpleWorld world = new SimpleWorld();

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String addressHex = entry.getKey();
            JsonObject accountJson = entry.getValue().getAsJsonObject();
            Logger.LOG("aa");
            // Create a new account
            Address address = Address.fromHexString(addressHex);
            Logger.LOG("bb");
            MutableAccount account = world.createAccount(address);
            Logger.LOG("cc");

            // Set balance
            String balanceStr = accountJson.get("balance").getAsString();
            // Remove the "0x" prefix if present
            if (balanceStr.startsWith("0x") || balanceStr.startsWith("0X")) {
                balanceStr = balanceStr.substring(2);
}
            BigInteger balance = new BigInteger(balanceStr, 16);
            account.setBalance(Wei.of(balance));
            Logger.LOG("dd");

            // If it's a contract account, set code and storage
            if (accountJson.has("code")) {
                String codeHex = accountJson.get("code").getAsString();
                account.setCode(Bytes.fromHexString(codeHex));
                
                // Set storage
                if (accountJson.has("storage")) {
                    JsonObject storageJson = accountJson.getAsJsonObject("storage");
                    for (Map.Entry<String, JsonElement> storageEntry : storageJson.entrySet()) {
                        UInt256 slot = UInt256.fromHexString(storageEntry.getKey());
                        UInt256 value = UInt256.fromHexString(storageEntry.getValue().getAsString());
                        account.setStorageValue(slot, value);
                    }
                }
            }
        }
        Logger.LOG("ee");
        return world;
    }

    @SuppressWarnings("unchecked")
    private JsonObject worldToJson(SimpleWorld state) {
        int count = 0;
        JsonObject stateObj = new JsonObject();

        Collection<SimpleAccount> simpleAccount = (Collection<SimpleAccount>) state.getTouchedAccounts();
        for (SimpleAccount account : simpleAccount) {
            JsonObject accountJson = new JsonObject();
            
            accountJson.addProperty("balance", account.getBalance().toString());
            
            // If it's a contract account, add code and storage
            if (account.getCode() != Bytes.EMPTY) {
                accountJson.addProperty("code", account.getCode().toHexString());
                
                JsonObject storageJson = new JsonObject();
                
                Map<UInt256, UInt256> storage = account.getUpdatedStorage();

                for (Map.Entry<UInt256, UInt256> entry : storage.entrySet()) {
                    UInt256 slot = entry.getKey();
                    UInt256 value = entry.getValue();
                    
                    if (!value.isZero()) {
                        storageJson.addProperty(slot.toHexString(), value.toHexString());
                    }
                }
                
                accountJson.add("storage", storageJson);
            }
            
            if (account.getAddress() != null) {
                stateObj.add(account.getAddress().toHexString(), accountJson);
            } else {
                //TODO: nao percebo o que leva isto ser executado
                // stateObj.add("unknow_account"+count, accountJson);
                // count++;
            }
        }
        return stateObj;
    }


}
