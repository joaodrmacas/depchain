package pt.tecnico.ulisboa.server;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.fluent.SimpleAccount;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.contracts.AbiParameter.AbiType;
import pt.tecnico.ulisboa.contracts.AbiParameter;
import pt.tecnico.ulisboa.contracts.Contract;
import pt.tecnico.ulisboa.contracts.ContractMethod;
import pt.tecnico.ulisboa.protocol.BalanceOfDepCoinReq;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.ClientReq.ClientReqType;
import pt.tecnico.ulisboa.protocol.ClientResp;
import pt.tecnico.ulisboa.protocol.ContractCallReq;
import pt.tecnico.ulisboa.protocol.TransferDepCoinReq;
import pt.tecnico.ulisboa.utils.ContractUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class BlockchainManager {
    private SimpleWorld world;

    protected ArrayList<Block> blockchain;
    BlockchainPersistenceManager persistenceManager;

    // map of client ids to their respective addresses
    protected Map<Integer, Address> clientAddresses;
    protected Map<String, Contract> contracts;
    private final EVMExecutor executor;
    private final ByteArrayOutputStream output;

    public BlockchainManager() {
        this.blockchain = new ArrayList<>();
        this.persistenceManager = new BlockchainPersistenceManager();
        this.output = new ByteArrayOutputStream();
        this.executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        this.initBlockchain();
    }

    private void initBlockchain() {
        try {
            // this.world = persistenceManager.loadBlockchain(blockchain);
            this.contracts = new HashMap<>();
            this.world = persistenceManager.loadGenesisBlock(blockchain, contracts); // this changes the blockchain and
                                                                                     // the contract map

            Block lastBlock = blockchain.get(blockchain.size() - 1);
            this.clientAddresses = new HashMap<>();
            for (Map.Entry<Integer, String> entry : Config.CLIENT_ID_2_ADDR.entrySet()) {
                clientAddresses.put(entry.getKey(), Address.fromHexString(entry.getValue()));
            }
        } catch (Exception e) {
            Logger.LOG("Failed to load blockchain: " + e.getMessage());
            this.world = new SimpleWorld();
            this.blockchain = new ArrayList<>();
        }
        executor.tracer(new StandardJsonTracer(new PrintStream(output), true, true, true, true));
        executor.worldUpdater(world.updater());
        executor.commitWorldState();
    }

    // Trasnfer depcoin from one account to another
    private ClientResp transferDepCoin(TransferDepCoinReq req) {
        // parse the args
        Address sender = clientAddresses.get(req.getSenderId());
        Address receiver = req.getReceiver();
        BigInteger amount = req.getAmount();

        WorldUpdater worldUpdater = (WorldUpdater) world;
        // if (world instanceof WorldUpdater) {
        // worldUpdater = (WorldUpdater) world;
        // } else {
        // // Get a proper updater from the world state
        // worldUpdater = world.updater();
        // }

        // Get accounts from the updater
        MutableAccount senderAccount = worldUpdater.getOrCreate(sender);
        MutableAccount receiverAccount = worldUpdater.getOrCreate(receiver);

        // Check balance
        // Convert amount to Wei. 1 DepCoin = 10^18 Wei
        Wei weiAmount = Wei.of(amount.multiply(BigInteger.TEN.pow(18)));
        if (senderAccount.getBalance().lessThan(weiAmount)) {
            Logger.LOG("Sender does not have enough balance");
            return new ClientResp(false, req.getCount(), "Sender does not have enough balance");
        }

        try {
            // Perform the transfer
            senderAccount.decrementBalance(weiAmount);
            receiverAccount.incrementBalance(weiAmount);

            // Commit changes to the world state
            worldUpdater.commit();
            return new ClientResp(true, req.getCount(), "Transfer from " + sender + " to " + receiver + " of " + amount
                    + " DepCoin completed successfully");
        } catch (Exception e) {
            // In case of any error, don't commit the changes
            Logger.LOG("Transaction failed: " + e.getMessage());
            return new ClientResp(false, req.getCount(), "Transfer from " + sender + " to " + receiver + " of " + amount
                    + " DepCoin failed: " + e.getMessage());
        }
    }

    private ClientResp getBalanceOfDepCoin(BalanceOfDepCoinReq req) {

        Logger.LOG(req.toString());

        Address address = Address.fromHexString(req.getAddress());

        MutableAccount account = world.getAccount(address);
        if (account == null) {
            Logger.LOG("Account not found: " + address);
            return new ClientResp(false, req.getCount(), "Account not found: " + address);
        }
        // Convert balance from Wei to DepCoin
        BigInteger balanceInWei = account.getBalance().toBigInteger();
        BigInteger balance = balanceInWei.divide(BigInteger.TEN.pow(18));
        return new ClientResp(true, req.getCount(), "Balance of " + address + ": " + balance);
    }

    public boolean needsConsensus(ClientReq tx) {
        if (tx.getReqType() == ClientReqType.CONTRACT_CALL) {
            ContractCallReq contractCallReq = (ContractCallReq) tx;
            Contract contract = contracts.get(contractCallReq.getContractName());
            if (contract != null) {
                ContractMethod method = contract.getMethod(contractCallReq.getMethodName());
                if (method != null && method.changesState()) {
                    return true;
                }
            }
            return false;
        } else if (tx.getReqType() == ClientReqType.TRANSFER_DEP_COIN) {
            return true;
        } else if (tx.getReqType() == ClientReqType.BALANCE_OF_DEP_COIN) {
            return false;
        } else {
            Logger.LOG("Invalid request type when checking if it should go to consensus");
            throw new IllegalArgumentException("Invalid request type when checking if it should go to consensus");
        }
    }

    public void addBlockToBlockchain(final Block block) {
        try {
            if (blockchain.size() == 0) {
                Logger.ERROR("Blockchain is empty, genesis block was not added");
            } else {
                blockchain.add(block);
                Block lastBlock = blockchain.get(blockchain.size() - 1);
                block.setId(lastBlock.getId() + 1);
            }
            blockchain.add(block);
            persistenceManager.persistBlock(block, world, contracts);
        } catch (Exception e) {
            Logger.ERROR("Failed to persis block to blockchain: " + e.getMessage());
        }
    }

    public ClientResp executeTx(ClientReq tx) {
        try {
            ClientReqType decidedType = tx.getReqType();
            ClientResp resp = null;

            switch (decidedType) {
                // these change the blockchain state
                case CONTRACT_CALL:
                    ContractCallReq contractCallReq = (ContractCallReq) tx;
                    resp = handleContractCall(contractCallReq);
                    break;
                case TRANSFER_DEP_COIN:
                    TransferDepCoinReq transferReq = (TransferDepCoinReq) tx;
                    resp = transferDepCoin(transferReq);
                    break;
                case BALANCE_OF_DEP_COIN:
                    BalanceOfDepCoinReq balanceReq = (BalanceOfDepCoinReq) tx;
                    resp = getBalanceOfDepCoin(balanceReq);
                    break;
                default:
                    System.out.println("Invalid request type");
                    return new ClientResp(false, tx.getCount(), "Invalid request type.");
            }
            return resp;
        } catch (Exception e) {
            Logger.LOG("Failed to handle decided value: " + e.getMessage());
            return new ClientResp(false, tx.getCount(), "Failed to handle decided value: " + e.getMessage());
        }
    }

    private ClientResp handleContractCall(ContractCallReq req) {
        try {
            // Validate contract and method
            Logger.LOG("CONTRACT CALL: " + req.toString());

            Contract contract = contracts.get(req.getContractName());
            if (contract == null) {
                Logger.LOG("Contract not found: " + req.getContractName());
                return new ClientResp(false, req.getCount(), "Contract not found: " + req.getContractName());
            }

            ContractMethod method = contract.getMethod(req.getMethodName());
            if (method == null) {
                Logger.LOG("Method not found: " + req.getMethodName());
                return new ClientResp(false, req.getCount(), "Method not found: " + req.getMethodName());
            }

            return executeCall(req, contract, method);
        } catch (Exception e) {
            Logger.LOG("Failed to execute contract call: " + e.getMessage());
            return new ClientResp(false, req.getCount(), "Failed to execute contract call: " + e.getMessage());
        }
    }

    private ClientResp executeCall(ContractCallReq req, Contract contract, ContractMethod method) {
        // Setup execution context
        setupExecutionContext(req, contract);

        // Prepare call data and execute
        Bytes callData = Bytes.concatenate(method.getSignature(), req.getArgs());
        executor.callData(callData);
        executor.execute();
        ContractUtils.checkForExecutionErrors(output);

        // Process return values
        String returnMessage = processReturnValues(method);

        Logger.LOG("Contract call executed successfully. " + returnMessage);
        return new ClientResp(true, req.getCount(), "Contract call executed successfully. " + returnMessage);
    }

    private void setupExecutionContext(ContractCallReq req, Contract contract) {
        // Set sender and value
        Address sender = clientAddresses.get(req.getSenderId());
        Wei depValue = req.getValue();
        executor.sender(sender);
        executor.ethValue(depValue); // Set contract address and code
        executor.receiver(contract.getAddress());
        MutableAccount contractAccount = world.getAccount(contract.getAddress());
        executor.code(contractAccount.getCode());
    }

    private String processReturnValues(ContractMethod method) {
        String returnMessage = "";

        for (int i = 0; i < method.getOutputs().size(); i++) {
            AbiParameter output = method.getOutputs().get(i);
            // print the output parameter
            Logger.LOG("Output type: " + output);
            String returnValue = extractReturnValue(output.getType());
            returnMessage += output.getName() + ": " + returnValue + "\n";
        }

        return returnMessage;
    }

    private String extractReturnValue(AbiType outputType) {
        switch (outputType) {
            case UINT256:
                Logger.LOG("Extracting UINT256 from return data");
                return ContractUtils.extractBigIntegerFromReturnData(output).toString();
            case BOOL:
                return ContractUtils.extractBooleanFromReturnData(output) ? "true" : "false";
            default:
                Logger.LOG("Unsupported output type: " + outputType);
                return null;
        }
    }

    public void printBlockchain() {
        System.out.println("BLOCKCHAIN:");
        for (int i = 0; i < blockchain.size(); i++) {
            Block block = blockchain.get(i);
            block.printBlock();
            System.out.println("   ↓");
        }
    }

    public boolean isValidBlock(Block block, Map<Integer, PublicKey> clientsPublicKeys) {
        Block lastBlock = blockchain.get(blockchain.size() - 1);

        if (lastBlock == null) {
            Logger.LOG("No last block");

            lastBlock = new Block(null, -1, null, new ArrayList<>());
        }

        if (!block.getId().equals(lastBlock.getId() + 1)) {
            Logger.LOG("Invalid block ID: " + block.getId() + ", expected: " + (lastBlock.getId() + 1));
        }

        if (!block.getPrevHash().equals(lastBlock.getHash())) {
            Logger.LOG("Invalid previous hash: " + block.getPrevHash() + ", expected: " + lastBlock.getHash());
            return false;
        }

        return block.isValid(clientsPublicKeys);
    }

    public Block generateNewBlock(List<ClientReq> txs) {
        return new Block(blockchain.get(blockchain.size() - 1).getHash(), blockchain.size(), txs);
    }
}