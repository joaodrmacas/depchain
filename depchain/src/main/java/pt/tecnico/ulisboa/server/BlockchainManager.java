package pt.tecnico.ulisboa.server;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.contracts.Contract;

import pt.tecnico.ulisboa.contracts.MergedContract;
import pt.tecnico.ulisboa.protocol.ApproveReq;
import pt.tecnico.ulisboa.protocol.BlacklistReq;
import pt.tecnico.ulisboa.protocol.CheckBalanceReq;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.ClientReq.ClientReqType;
import pt.tecnico.ulisboa.protocol.ClientResp;
import pt.tecnico.ulisboa.protocol.GetAllowanceReq;
import pt.tecnico.ulisboa.protocol.IsBlacklistedReq;
import pt.tecnico.ulisboa.protocol.TransferFromReq;
import pt.tecnico.ulisboa.protocol.TransferReq;
import pt.tecnico.ulisboa.utils.types.Logger;

public class BlockchainManager<T> {
    private Account adminAccount;
    private SimpleWorld world;
    private MergedContract mergedContract;
    private ArrayList<Block> blockchain;
    private Block currentBlock;

    // map of client ids to their respective addresses
    private Map<Address, Contract> contracts;
    private Map<Integer, Account> clientAccounts;

    public BlockchainManager() {
        this.world = new SimpleWorld();
        this.clientAccounts = Config.CLIENT_ID_2_ADDR; // TODO: change this to read genesis block
        this.adminAccount = clientAccounts.get(Config.ADMIN_ID);
        this.mergedContract = new MergedContract(world); // TODO: change this to read genesis block
    }

    public ClientResp handleDecidedValue(final T value) {
        try {
            ClientReq decided = (ClientReq) value;
            ClientReqType decidedType = decided.getReqType();

            switch (decidedType) {
                // these change the blockchain state
                case TRANSFER:
                    TransferReq transferReq = (TransferReq) decided;
                    return handleTransfer(transferReq);
                case TRANSFER_FROM:
                    TransferFromReq transferFromReq = (TransferFromReq) decided;
                    return handleTransferFrom(transferFromReq);
                case APPROVE:
                    ApproveReq approveReq = (ApproveReq) decided;
                    return handleApprove(approveReq);
                case BLACKLIST:
                    BlacklistReq blacklistReq = (BlacklistReq) decided;
                    return handleBlacklist(blacklistReq);
                // these do not change the blockchain state
                case IS_BLACKLISTED:
                    IsBlacklistedReq isBlacklistedReq = (IsBlacklistedReq) decided;
                    return handleIsBlackListed(isBlacklistedReq);
                case CHECK_BALANCE:
                    CheckBalanceReq checkBalanceReq = (CheckBalanceReq) decided;
                    return handleCheckBalance(checkBalanceReq);
                case GET_ALLOWANCE:
                    GetAllowanceReq getAllowanceReq = (GetAllowanceReq) decided;
                    return handleGetAllowance(getAllowanceReq);
                default:
                    System.out.println("Invalid request type");
                    return new ClientResp(false, null, "Invalid request type.");
            }
            addTransactionToBlock(new Transaction(decided));
        } catch (Exception e) {
            Logger.LOG("Failed to handle decided value: " + e.getMessage());
            return new ClientResp(false, null, "Failed to handle decided value: " + e.getMessage());
        }
    }

    // ######### These methods will not change the blockchain state #########
    private ClientResp handleIsBlackListed(IsBlacklistedReq req) {
        try {
            Address sender = clientAccounts.get(req.getSender()).getAddress();
            Address toCheck = clientAccounts.get(req.getToCheck()).getAddress();

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
            Address client = clientAccounts.get(req.getAccount()).getAddress();
            BigInteger balance = mergedContract.balanceOf(client);

            return new ClientResp(true, LocalDateTime.now(),
                    "Balance of client " + req.getAccount() + " is " + balance + ".");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process checkBalance request for client "
                    + req.getAccount() + ". Reason: " + e.getMessage());
        }
    }

    private ClientResp handleGetAllowance(GetAllowanceReq req) {
        try {
            Address allower = clientAccounts.get(req.getAllower()).getAddress();
            Address allowee = clientAccounts.get(req.getAllowee()).getAddress();

            BigInteger allowance = mergedContract.allowance(allower, allowee);

            return new ClientResp(true, LocalDateTime.now(),
                    "Allowance from " + req.getAllower() + " to " + req.getAllowee() + " is " + allowance + ".");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process getAllowance request from " + req.getAllower()
                    + " to " + req.getAllowee() + ". Reason: " + e.getMessage());
        }
    }

    // ########## These methods will change the blockchain state ##########
    private ClientResp handleTransfer(TransferReq req) {
        try {
            Address from = clientAccounts.get(req.getFrom()).getAddress();
            Address to = clientAccounts.get(req.getTo()).getAddress();

            mergedContract.transfer(from, to, req.getAmount());

            return new ClientResp(true, LocalDateTime.now(),
                    "Transfer from " + req.getFrom() + " to " + req.getTo() + " processed successfully.");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process transfer request from " + req.getFrom() + " to "
                    + req.getTo() + ". Reason: " + e.getMessage());
        }
    }

    private ClientResp handleTransferFrom(TransferFromReq req) {
        try {
            Address sender = clientAccounts.get(req.getSender()).getAddress();
            Address from = clientAccounts.get(req.getFrom()).getAddress();
            Address to = clientAccounts.get(req.getTo()).getAddress();

            mergedContract.transferFrom(sender, from, to, req.getAmount());

            return new ClientResp(true, LocalDateTime.now(),
                    "Transfer from " + req.getFrom() + " to " + req.getTo() + " processed successfully.");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process transferFrom request from " + req.getFrom()
                    + " to " + req.getTo() + ". Reason: " + e.getMessage());
        }
    }

    private ClientResp handleBlacklist(BlacklistReq req) {
        try {
            Address sender = clientAccounts.get(req.getSender()).getAddress();
            Address toBlacklist = clientAccounts.get(req.getToBlacklist()).getAddress();

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
            Address allower = clientAccounts.get(req.getAllower()).getAddress();
            Address allowee = clientAccounts.get(req.getAllowee()).getAddress();

            mergedContract.approve(allower, allowee, req.getAmount());

            return new ClientResp(true, LocalDateTime.now(),
                    "Approval from " + req.getAllower() + " to " + req.getAllowee() + " for " + req.getAmount()
                            + " processed successfully.");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process approval request from " + req.getAllower() + " to "
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