package pt.tecnico.ulisboa.server;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.ethereum.core.MutableWorldState;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.contracts.Contract;

import pt.tecnico.ulisboa.contracts.MergedContract;
import pt.tecnico.ulisboa.protocol.ApproveReq;
import pt.tecnico.ulisboa.protocol.BlacklistReq;
import pt.tecnico.ulisboa.protocol.BuyISTReq;
import pt.tecnico.ulisboa.protocol.CheckBalanceReq;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.ClientReq.ClientReqType;
import pt.tecnico.ulisboa.protocol.ClientResp;
import pt.tecnico.ulisboa.protocol.GetAllowanceReq;
import pt.tecnico.ulisboa.protocol.IsBlacklistedReq;
import pt.tecnico.ulisboa.protocol.TransferFromReq;
import pt.tecnico.ulisboa.protocol.TransferReq;
import pt.tecnico.ulisboa.utils.ContractUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class BlockchainManager<T> {
    private Address adminAddress;
    private SimpleWorld world;
    private MergedContract mergedContract;
    private ArrayList<Block> blockchain;
    private Block currentBlock;

    // map of client ids to their respective addresses
    private Map<Address, Contract> contracts;
    private Map<Integer, Address> clientAddresses;

    public BlockchainManager() {
        this.world = new SimpleWorld();

        // TODO: change this to read genesis block
        this.clientAddresses = Config.CLIENT_ID_2_ADDR;

        this.adminAddress = clientAddresses.get(Config.ADMIN_ID);

        // TODO: change this to read genesis block
        Address contractAddr = Address.fromHexString(Config.MERGED_CONTRACT_ADDRESS);
        this.mergedContract = new MergedContract(contractAddr, this.world);
    }

    // Trasnfer depcoin from one account to another
    // TODO: handler that will call this
    public boolean transferDepCoin(Address sender, Address receiver, BigInteger amount) {
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
            return false;
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
                case TRANSFER:
                    TransferReq transferReq = (TransferReq) decided;
                    resp = handleTransferISTCoin(transferReq);
                    break;
                case TRANSFER_FROM:
                    TransferFromReq transferFromReq = (TransferFromReq) decided;
                    resp = handleTransferFrom(transferFromReq);
                    break;
                case APPROVE:
                    ApproveReq approveReq = (ApproveReq) decided;
                    resp = handleApprove(approveReq);
                    break;
                case BLACKLIST:
                    BlacklistReq blacklistReq = (BlacklistReq) decided;
                    resp = handleBlacklist(blacklistReq);
                    break;
                // these do not change the blockchain state
                case IS_BLACKLISTED:
                    IsBlacklistedReq isBlacklistedReq = (IsBlacklistedReq) decided;
                    resp = handleIsBlackListed(isBlacklistedReq);
                    break;
                case CHECK_BALANCE:
                    CheckBalanceReq checkBalanceReq = (CheckBalanceReq) decided;
                    resp = handleCheckBalance(checkBalanceReq);
                    break;
                case GET_ALLOWANCE:
                    GetAllowanceReq getAllowanceReq = (GetAllowanceReq) decided;
                    resp = handleGetAllowance(getAllowanceReq);
                    break;
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

    // fucntion to swap depcoin for istcoin
    private ClientResp handleBuyIST(BuyISTReq req) {
        try {
            Address sender = clientAddresses.get(req.getSenderId());

            // check if sender has enough depcoin
            Wei depCoinAmount = Wei.of(req.getAmount());
            MutableAccount senderAccount = (MutableAccount) ContractUtils.getAccountFromAddress(world, sender);
            if (senderAccount.getBalance().lessThan(depCoinAmount)) {
                return new ClientResp(false, null,
                        "Client " + req.getSenderId() + " does not have enough DepCoin to buy " + req.getAmount()
                                + " IST. Amount required: " + depCoinAmount);
            }

            mergedContract.buy(sender, req.getAmount());
            return new ClientResp(true, LocalDateTime.now(),
                    "Client " + req.getSenderId() + " successfully bought " + req.getAmount() + " IST for "
                            + usedDepCoin + " DepCoin.");

        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process swap request from " + req.getSenderId() + " to "
                    + req.getReceiverId() + ". Reason: " + e.getMessage());
        }
    }

    private ClientResp handleIsBlackListed(IsBlacklistedReq req) {
        try {
            Address sender = clientAddresses.get(req.getSenderId());
            Address toCheck = clientAddresses.get(req.getToCheck());

            boolean isBlacklisted = mergedContract.isBlacklisted(sender, toCheck);

            return new ClientResp(true, LocalDateTime.now(),
                    "Client " + req.getToCheck() + " is " + (isBlacklisted ? "" : "not ") + "blacklisted.");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process isBlacklisted request for client "
                    + req.getToCheck() + ". Reason: " + e.getMessage());
        }
    }

    private ClientResp handleCheckBalance(CheckBalanceReq req) {
        try {
            Address client = clientAddresses.get(req.getSenderId());

            BigInteger balance = mergedContract.balanceOf(client);

            return new ClientResp(true, LocalDateTime.now(),
                    "Balance of client " + req.getSenderId() + " is " + balance + ".");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process checkBalance request for client "
                    + req.getSenderId() + ". Reason: " + e.getMessage());
        }
    }

    private ClientResp handleGetAllowance(GetAllowanceReq req) {
        try {
            Address allower = clientAddresses.get(req.getAllower());
            Address allowee = clientAddresses.get(req.getSenderId());

            BigInteger allowance = mergedContract.allowance(allower, allowee);

            return new ClientResp(true, LocalDateTime.now(),
                    "Allowance from " + req.getAllower() + " to " + req.getSenderId() + " is " + allowance + ".");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process getAllowance request from " + req.getAllower()
                    + " to " + req.getSenderId() + ". Reason: " + e.getMessage());
        }
    }

    private ClientResp handleTransferISTCoin(TransferReq req) {
        try {
            Address from = clientAddresses.get(req.getSenderId());
            Address to = clientAddresses.get(req.getTo());

            mergedContract.transfer(from, to, req.getAmount());

            return new ClientResp(true, LocalDateTime.now(),
                    "Transfer from " + req.getSenderId() + " to " + req.getTo() + " processed successfully.");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process transfer request from " + req.getSenderId() + " to "
                    + req.getTo() + ". Reason: " + e.getMessage());
        }
    }

    private ClientResp handleTransferFrom(TransferFromReq req) {
        try {
            Address sender = clientAddresses.get(req.getSenderId());
            Address from = clientAddresses.get(req.getFrom());
            Address to = clientAddresses.get(req.getTo());

            mergedContract.transferFrom(sender, from, to, req.getAmount());

            return new ClientResp(true, LocalDateTime.now(),
                    "Transfer from " + req.getFrom() + " to " + req.getTo() + " processed successfully by "
                            + req.getSenderId() + ".");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process transferFrom request from " + req.getFrom()
                    + " to " + req.getTo() + " by " + req.getSenderId() + ". Reason: " + e.getMessage());
        }
    }

    private ClientResp handleBlacklist(BlacklistReq req) {
        try {
            Address sender = clientAddresses.get(req.getSenderId());
            Address toBlacklist = clientAddresses.get(req.getToBlacklist());

            if (req.isToBlacklist()) {
                mergedContract.addToBlacklist(sender, toBlacklist);
                return new ClientResp(true, LocalDateTime.now(),
                        "Client " + req.getToBlacklist() + " successfully added to blacklist.");
            } else {
                mergedContract.removeFromBlacklist(sender, toBlacklist);
                return new ClientResp(true, LocalDateTime.now(),
                        "Client " + req.getToBlacklist() + " successfully removed from blacklist.");
            }

        } catch (Exception e) {
            return new ClientResp(false, null,
                    "Failed to process blacklist request for client " + req.getToBlacklist()
                            + ". Reason: " + e.getMessage());
        }
    }

    private ClientResp handleApprove(ApproveReq req) {
        try {
            Address allower = clientAddresses.get(req.getSenderId());
            Address allowee = clientAddresses.get(req.getAllowee());

            mergedContract.approve(allower, allowee, req.getAmount());

            return new ClientResp(true, LocalDateTime.now(),
                    "Approval from " + req.getSenderId() + " to " + req.getAllowee() + " for " + req.getAmount()
                            + " processed successfully.");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process approval request from " + req.getSenderId() + " to "
                    + req.getAllowee() + ". Reason: " + e.getMessage());
        }
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