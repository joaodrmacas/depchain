package pt.tecnico.ulisboa;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;

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
    private Address admin;
    private SimpleWorld world;
    private MergedContract mergedContract;
    private ArrayList<Block> blockchain;

    // map of client ids to their respecetive addresses
    private Map<Integer, Address> clientAddresses;

    public BlockchainManager() {
        this.world = new SimpleWorld();
        this.clientAddresses = Config.CLIENT_ID_2_ADDR;
        this.admin = clientAddresses.get(Config.ADMIN_ID);
        this.mergedContract = new MergedContract(world);
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
        } catch (Exception e) {
            Logger.LOG("Failed to handle decided value: " + e.getMessage());
            return new ClientResp(false, null, "Failed to handle decided value: " + e.getMessage());
        }
    }

    // these methods will not change the blockchain state
    private ClientResp handleIsBlackListed(IsBlacklistedReq req) {
        try {
            Address sender = clientAddresses.get(req.getSender());
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
            Address client = clientAddresses.get(req.getAccount());
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
            Address allower = clientAddresses.get(req.getAllower());
            Address allowee = clientAddresses.get(req.getAllowee());

            BigInteger allowance = mergedContract.allowance(allower, allowee);

            return new ClientResp(true, LocalDateTime.now(),
                    "Allowance from " + req.getAllower() + " to " + req.getAllowee() + " is " + allowance + ".");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process getAllowance request from " + req.getAllower()
                    + " to " + req.getAllowee() + ". Reason: " + e.getMessage());
        }
    }

    // these methods will change the blockchain state
    private ClientResp handleTransfer(TransferReq req) {
        try {
            Address from = clientAddresses.get(req.getFrom());
            Address to = clientAddresses.get(req.getTo());

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
            Address sender = clientAddresses.get(req.getSender());
            Address from = clientAddresses.get(req.getFrom());
            Address to = clientAddresses.get(req.getTo());

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
            Address sender = clientAddresses.get(req.getSender());
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
            Address allower = clientAddresses.get(req.getAllower());
            Address allowee = clientAddresses.get(req.getAllowee());

            mergedContract.approve(allower, allowee, req.getAmount());

            return new ClientResp(true, LocalDateTime.now(),
                    "Approval from " + req.getAllower() + " to " + req.getAllowee() + " for " + req.getAmount()
                            + " processed successfully.");
        } catch (Exception e) {
            return new ClientResp(false, null, "Failed to process approval request from " + req.getAllower() + " to "
                    + req.getAllowee() + ". Reason: " + e.getMessage());
        }
    }

    private void addTransaction(ClientReq req) {
        Block lastBlock = blockchain.get(blockchain.size() - 1);
        if (lastBlock.isFull()) {
            String lastBlockHash = lastBlock.finalizeBlock(world);
            lastBlock = new Block(lastBlockHash);
            blockchain.add(lastBlock);
        }
        lastBlock.appendTransaction(req);
    }

}