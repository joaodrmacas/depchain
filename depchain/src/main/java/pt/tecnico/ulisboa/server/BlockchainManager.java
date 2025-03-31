package pt.tecnico.ulisboa.server;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
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
import org.hyperledger.besu.evm.fluent.SimpleAccount;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.contracts.Contract;
import pt.tecnico.ulisboa.contracts.MergedContract;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.ClientReq.ClientReqType;
import pt.tecnico.ulisboa.protocol.ClientResp;
import pt.tecnico.ulisboa.protocol.ContractCallReq;
import pt.tecnico.ulisboa.utils.ContractUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class BlockchainManager<T> {
    private SimpleWorld world;

    private ArrayList<Block> blockchain;
    private Block currentBlock;

    // map of client ids to their respective addresses
    private Map<Address, Address> contracts = new HashMap<>();
    private Map<Integer, Address> clientAddresses;

    private final EVMExecutor executor;
    private final ByteArrayOutputStream output;

    public BlockchainManager() {
        this.world = new SimpleWorld();

        // TODO: change this to read genesis block
        this.clientAddresses = Config.CLIENT_ID_2_ADDR;

        this.executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        this.output = new ByteArrayOutputStream();

        executor.tracer(new StandardJsonTracer(new PrintStream(output), true, true, true, true));
        executor.worldUpdater(world.updater());
        executor.commitWorldState();
    }

    // Trasnfer depcoin from one account to another
    // TODO: handler that will call this
    public ClientResp transferDepCoin(TransferDepCoinReq req) {
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
            return new ClientResp(false, req.)
        }

        try {
            // Perform the transfer
            senderAccount.decrementBalance(weiAmount);
            receiverAccount.incrementBalance(weiAmount);

            // Commit changes to the world state
            worldUpdater.commit();
            return true;
        } catch (Exception e) {
            // In case of any error, don't commit the changes
            Logger.LOG("Transaction failed: " + e.getMessage());
            return false;
        }
    }

    public ClientResp handleDecidedValue(final T value) {
        try {
            ClientReq decided = (ClientReq) value;
            ClientReqType decidedType = decided.getReqType();
            ClientResp resp = null;

            switch (decidedType) {
                // these change the blockchain state
                case CONTRACT_CALL:
                    ContractCallReq contractCallReq = (ContractCallReq) decided;
                    resp = handleContractCall(contractCallReq);
                    break;
                // TODO: case for depcoin transfer
                default:
                    System.out.println("Invalid request type");
                    return new ClientResp(false, null, "Invalid request type.");
            }
            if (resp.getSuccess()) {
                // TODO: Check if every transaction should be added to the blockchain
                addTransactionToBlock(new Transaction(decided));
            }
            return resp;
        } catch (Exception e) {
            Logger.LOG("Failed to handle decided value: " + e.getMessage());
            return new ClientResp(false, null, "Failed to handle decided value: " + e.getMessage());
        }
    }

    private ClientResp handleContractCall(ContractCallReq req) {
        try {
            // msg.sender and msg.value
            Address sender = clientAddresses.get(req.getSenderId());
            BigInteger value = req.getValue();

            // contract and call data
            Address contractAddress = req.getContractAddr();
            Bytes callData = Bytes.fromHexString(req.getMethodSelector() + req.getArgs()); // TODO: should be good might
                                                                                           // be wrong

            // set parameters in the executor
            executor.sender(sender);
            executor.ethValue(Wei.of(value));
            executor.contract(contractAddress);
            executor.callData(callData);

            // execute the transaction
            executor.execute();

            return new ClientResp(true, req.getCount(), "Transaction executed successfully");
        } catch (Exception e) {
            Logger.LOG("Failed to execute contract call: " + e.getMessage());
            return new ClientResp(false, req.getCount(), "Failed to execute contract call: " + e.getMessage());
        }
    }

    public void addContract(Address addr, String deployCode, String byteCode) {
        this.world.createAccount(addr, 0, Wei.fromEth(0));

        try {
            executor.code(Bytes.fromHexString(deployCode));
            executor.callData(Bytes.EMPTY);
            executor.execute();

            ContractUtils.checkForExecutionErrors(this.output);

            // Update to runtime bytecode (@carrao mudei isto para o code ficar na conta do
            // contrato, nao sei se isto vai dar merda)
            // TODO: testar pode dar merda
            SimpleAccount account = (SimpleAccount) world.getAccount(addr);
            account.setCode(Bytes.fromHexString(byteCode));

            Logger.LOG("Merged contract deployed successfully");
        } catch (Exception e) {
            Logger.LOG("Failed to deploy Merged contract: " + e.getMessage());
            throw new RuntimeException("Deployment failed", e);
        }

        contracts.put(addr, addr);
    }

    private void addTransactionToBlock(Transaction req) {
        currentBlock.appendTransaction(req);
        if (currentBlock.isFull()) {
            currentBlock.finalizeBlock(world);
            blockchain.add(currentBlock);
            String prev_hash = currentBlock.getBlockHash();
            currentBlock = new Block(prev_hash);
        }
    }

}