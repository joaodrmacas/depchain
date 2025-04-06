package pt.tecnico.ulisboa.server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
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
import pt.tecnico.ulisboa.contracts.AbiParameter;
import pt.tecnico.ulisboa.contracts.AbiParameter.AbiType;
import pt.tecnico.ulisboa.contracts.Contract;
import pt.tecnico.ulisboa.contracts.ContractMethod;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.ClientReqFactory;
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

    public SimpleWorld loadGenesisBlock(List<Block> blockchain, Map<String, Contract> contracts) throws IOException {
        try (FileReader reader = new FileReader(genesisBlockFile)) {

            JsonObject rootObj = JsonParser.parseReader(reader).getAsJsonObject();
            Block genesisBlock = new Block();
            blockchain.add(genesisBlock);

            SimpleWorld world = worldFromJson(rootObj.getAsJsonObject("state"), contracts);
            return world;

        } catch (IOException e) {
            throw new IOException("Failed to load genesis block: " + e.getMessage(), e);
        }
    }

    private SimpleWorld loadBlock(int blockId, List<Block> blockchain, Map<String, Contract> contracts)
            throws IOException {
        String blockFileName = String.format("%s/block_%d.json", dataDirectory, blockId);
        try (FileReader reader = new FileReader(blockFileName)) {
            JsonObject rootObj = JsonParser.parseReader(reader).getAsJsonObject();

            // Load the block
            String blockHash = rootObj.get("block_hash").getAsString();
            String prevHash = rootObj.has("previous_block_hash") ? rootObj.get("previous_block_hash").getAsString()
                    : null;
            JsonArray txArray = rootObj.getAsJsonArray("transactions");
            List<ClientReq> transactions = new ArrayList<>();
            for (JsonElement txElement : txArray) {
                ClientReq tx = ClientReqFactory.fromJson(txElement.getAsJsonObject());
                transactions.add(tx);
            }
            blockchain.add(new Block(prevHash, blockId, blockHash, transactions));
            SimpleWorld world = worldFromJson(rootObj.getAsJsonObject("state"), contracts);
            return world;

        } catch (IOException e) {
            throw new IOException("Failed to load block: " + e.getMessage(), e);
        }
    }

    public void persistBlock(Block block, SimpleWorld world, Map<String, Contract> contracts) throws IOException {
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
        for (ClientReq tx : block.getTransactions()) {
            txArray.add(tx.toJson());
        }
        rootObj.add("transactions", txArray);

        // Add world state
        JsonObject stateObj = worldToJson(world, contracts);

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
                Logger.LOG("Previous hash mismatch: expected " + previousBlock.getHash() + ", got "
                        + currentBlock.getPrevHash());
                return false;
            }
        }

        Logger.LOG("Blockchain integrity verified successfully");
        return true;
    }

    public SimpleWorld loadBlockchain(List<Block> blockchain, Map<String, Contract> contracts)
            throws IOException {
        blockchain.clear();

        File dir = new File(dataDirectory);

        if (!new File(genesisBlockFile).exists()) {
            throw new IOException("Genesis block file not found: " + genesisBlockFile);
        }

        SimpleWorld currentWorld = loadGenesisBlock(blockchain, contracts);

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
                currentWorld = loadBlock(blockId, blockchain, contracts);
            }
        }

        return currentWorld;
    }

    private SimpleWorld worldFromJson(JsonObject jsonObject, Map<String, Contract> contracts) {
        SimpleWorld world = new SimpleWorld();

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String addressHex = entry.getKey();
            JsonObject accountJson = entry.getValue().getAsJsonObject();
            // Create a new account
            Address address = Address.fromHexString(addressHex);
            MutableAccount account = world.createAccount(address);

            // Set balance
            String balanceStr = accountJson.get("balance").getAsString();
            // Remove the "0x" prefix if present
            if (balanceStr.startsWith("0x") || balanceStr.startsWith("0X")) {
                balanceStr = balanceStr.substring(2);
            }
            BigInteger balance = new BigInteger(balanceStr, 16);
            account.setBalance(Wei.of(balance));

            // If it's a contract account, set th other parameters
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
                if (!accountJson.has("name")) {
                    Logger.ERROR("Missing contract name");
                }
                String contractName = accountJson.get("name").getAsString();

                if (accountJson.has("functions")) {
                    HashMap<String, ContractMethod> functions = new HashMap<>();
                    JsonObject functionsJson = accountJson.getAsJsonObject("functions");
                    for (Map.Entry<String, JsonElement> functionEntry : functionsJson.entrySet()) {
                        String functionName = functionEntry.getKey();
                        JsonObject functionParams = functionEntry.getValue().getAsJsonObject();
                        String functionSignature = functionParams.get("signature").getAsString();

                        // Get the outputs as a JsonArray (since it's a list of objects)
                        JsonArray functionOutputs = functionParams.getAsJsonArray("outputs");

                        // Convert the outputs into a List of AbiParameter
                        List<AbiParameter> outputsList = new ArrayList<>();
                        for (JsonElement outputElement : functionOutputs) {
                            JsonObject outputJson = outputElement.getAsJsonObject();
                            String type = outputJson.get("type").getAsString();
                            String name = outputJson.get("name").getAsString();
                            AbiType abiType = AbiType.valueOf(type.toUpperCase());
                            AbiParameter abiParameter = new AbiParameter(name, abiType);
                            outputsList.add(abiParameter);
                            // Add the parameter to the contract's method
                            // Assuming you have a way to add parameters to the contract's method
                        }

                        boolean changesState = functionParams.get("changesState").getAsBoolean();

                        // Now you have functionName, functionSignature, abiParameters, and changesState
                        ContractMethod function = new ContractMethod(functionSignature, outputsList, changesState);
                        functions.put(functionName, function);
                    }
                    Contract contract = new Contract(addressHex, functions);
                    contracts.put(contractName, contract);
                }
            }
        }
        return world;
    }

    @SuppressWarnings("unchecked")
    private JsonObject worldToJson(SimpleWorld state, Map<String, Contract> contracts) {
        int count = 0;
        JsonObject stateObj = new JsonObject();

        Collection<SimpleAccount> simpleAccount = (Collection<SimpleAccount>) state.getTouchedAccounts();
        for (SimpleAccount account : simpleAccount) {
            JsonObject accountJson = new JsonObject();

            accountJson.addProperty("balance", account.getBalance().toString());

            // If it's a contract account, add code and storage
            if (account.getCode() != Bytes.EMPTY) {
                // code
                accountJson.addProperty("code", account.getCode().toHexString());

                // storage
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

                String contractName = "ISTContract";
                accountJson.addProperty("name", contractName);


                // functions
                JsonObject functionsJson = new JsonObject();

                Contract c = contracts.get(contractName);
                if (c!=null){
                    HashMap<String, ContractMethod> functions = c.getMethods();
                    if (functions != null){
                        for (Map.Entry<String, ContractMethod> functionEntry : functions.entrySet()) {
                            String functionName = functionEntry.getKey();
                            ContractMethod function = functionEntry.getValue();
        
                            JsonObject functionJson = new JsonObject();
                            functionJson.addProperty("signature", function.getSignature().toHexString());
        
                            // Convert outputs to JsonArray
                            JsonArray outputsArray = new JsonArray();
                            for (AbiParameter output : function.getOutputs()) {
                                JsonObject outputJson = new JsonObject();
                                outputJson.addProperty("type", output.getType().toString());
                                outputJson.addProperty("name", output.getName());
                                outputsArray.add(outputJson);
                            }
                            functionJson.add("outputs", outputsArray);
                            functionJson.addProperty("changesState", function.changesState());
        
                            functionsJson.add(functionName, functionJson);
                        }
                    } else {
                        Logger.LOG("Didnt find contract methods for contract: " + contractName);
                    }
                } else {
                    Logger.LOG("Didnt find contract for contract: " + contractName);
                }
            }

            if (account.getAddress() != null) {
                stateObj.add(account.getAddress().toHexString(), accountJson);
            } else {
                // stateObj.add("unknow_account"+count, accountJson);
                // count++;
            }
        }
        return stateObj;
    }

}
