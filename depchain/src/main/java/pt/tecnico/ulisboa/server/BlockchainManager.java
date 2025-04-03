package pt.tecnico.ulisboa.server;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.core.MutableWorldState;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.ClientReq.ClientReqType;
import pt.tecnico.ulisboa.protocol.ClientResp;
import pt.tecnico.ulisboa.protocol.ContractCallReq;
import pt.tecnico.ulisboa.protocol.TransferDepCoinReq;
import pt.tecnico.ulisboa.utils.ContractUtils;
import pt.tecnico.ulisboa.utils.types.Logger;
import pt.tecnico.ulisboa.contracts.AbiParameter;
import pt.tecnico.ulisboa.contracts.AbiParameter.AbiType;
import pt.tecnico.ulisboa.contracts.Contract;
import pt.tecnico.ulisboa.contracts.ContractMethod;

public class BlockchainManager {
    private SimpleWorld world;

    private ArrayList<Block> blockchain;
    BlockchainPersistenceManager persistenceManager;

    // map of client ids to their respective addresses
    private Map<Integer, Address> clientAddresses;
    private Map<String, Contract> contracts;
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
            this.world = persistenceManager.loadGenesisBlock(blockchain); // this changes the blockchain
            Block lastBlock = blockchain.get(blockchain.size() - 1);
            this.currentBlock = new Block(lastBlock.getId() + 1, lastBlock.getHash());
            this.clientAddresses = new HashMap<>();
            for (Map.Entry<Integer, String> entry : Config.CLIENT_ID_2_ADDR.entrySet()) {
                clientAddresses.put(entry.getKey(), Address.fromHexString(entry.getValue()));
            }
            this.contracts = new HashMap<>();
        } catch (Exception e) {
            Logger.LOG("Failed to load blockchain: " + e.getMessage());
            this.world = new SimpleWorld();
            this.blockchain = new ArrayList<>();
            this.currentBlock = new Block();
        }
        executor.tracer(new StandardJsonTracer(new PrintStream(output), true, true, true, true));
        executor.worldUpdater(world.updater());
        executor.commitWorldState();
    }

    // Trasnfer depcoin from one account to another
    public ClientResp transferDepCoin(TransferDepCoinReq req) {
        // parse the args
        Address sender = clientAddresses.get(req.getSenderId());
        Address receiver = req.getReceiver();
        BigInteger amount = req.getAmount();

        // Get the current world state
        MutableWorldState worldState = (MutableWorldState) world;

        // Create a transaction context
        WorldUpdater worldUpdater = worldState.updater();

        // Get accounts from the updater
        MutableAccount senderAccount = worldUpdater.getOrCreate(sender);
        MutableAccount receiverAccount = worldUpdater.getOrCreate(receiver);

        // Check balance
        Wei weiAmount = Wei.of(amount);
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

    public void addBlockToBlockchain(final Block block) {
        Block lastBlock = blockchain.get(blockchain.size() - 1);
        block.setPrevHash(lastBlock.getHash());
        block.setId(lastBlock.getId() + 1);
        blockchain.add(block);
    }

    public ClientResp executeTx(ClientReq tx) {
        // TODO: change this logic to the executor thread and make this get a block, set
        // the previous hash of the previous hash
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
                default:
                    System.out.println("Invalid request type");
                    return new ClientResp(false, tx.getCount(), "Invalid request type.");
            }
            return resp;
        } catch (Exception e) {
            Logger.LOG("Failed to handle decided value: " + e.getMessage());
            return new ClientResp(false, null, "Failed to handle decided value: " + e.getMessage());
        }
    }

    private ClientResp handleContractCall(ContractCallReq req) {
        try {
            // get the contract and method
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

            if (method.changesState()) {
                // TODO: go to consensus
            } else {
                // execute the contract call

                // msg.sender and msg.value
                Address sender = clientAddresses.get(req.getSenderId());
                BigInteger value = req.getValue();
                executor.sender(sender);
                executor.ethValue(Wei.of(value));

                // address and code
                executor.contract(contract.getAddress());
                MutableAccount contractAccount = world.getAccount(contract.getAddress());
                executor.code(contractAccount.getCode());

                // call data
                Bytes callData = Bytes.concatenate(method.getSignature(), req.getArgs());
                executor.callData(callData);

                executor.execute();

                ContractUtils.checkForExecutionErrors(output);

                // List of objects to hold the return values
                ArrayList<Object> returnValues = new ArrayList<>();
                for (int i = 0; i < method.getOutputs().size(); i++) {
                    AbiType outputType = method.getOutputs().get(i).getType();
                    switch (outputType) {
                        case UINT256:
                            BigInteger returnValue = ContractUtils.extractBigIntegerFromReturnData(output);
                            break;
                        case INT256:
                            BigInteger returnValue = ContractUtils.extractBigIntegerFromReturnData(output);
                            break;
                        case BOOL:
                            boolean returnValue = ContractUtils.extractBooleanFromReturnData(output);
                            // handle bool output
                            break;
                            break;
                        default:
                            Logger.LOG("Unsupported output type: " + output.getType());
                    }
                }

                String returnValue = ContractUtils.extractHexStringFromReturnData(output);

                Logger.LOG("Contract call executed successfully. Return value: " + returnValue);

                return new ClientResp(true, req.getCount(), "Contract call executed successfully");

            }
        } catch (Exception e) {
            Logger.LOG("Failed to execute contract call: " + e.getMessage());
            return new ClientResp(false, req.getCount(), "Failed to execute contract call: " + e.getMessage());
        }
    }

    public void printBlockchain() {
        System.out.println("BLOCKCHAIN:");
        for (int i = 0; i < blockchain.size(); i++) {
            Block block = blockchain.get(i);
            block.printBlock();
            System.out.println("   ↓");
        }
        currentBlock.printBlock();

    }

}