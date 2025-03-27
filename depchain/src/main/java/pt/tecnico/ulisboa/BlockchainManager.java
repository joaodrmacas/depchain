package pt.tecnico.ulisboa;

import java.time.LocalDateTime;

import java.util.ArrayList;

import org.checkerframework.checker.units.qual.A;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;

import pt.tecnico.ulisboa.contracts.MergedContract;
import pt.tecnico.ulisboa.protocol.ApproveReq;
import pt.tecnico.ulisboa.protocol.BlacklistReq;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.ClientReq.ClientReqType;
import pt.tecnico.ulisboa.protocol.ClientResp;
import pt.tecnico.ulisboa.protocol.TransferFromReq;
import pt.tecnico.ulisboa.protocol.TransferReq;
import pt.tecnico.ulisboa.utils.ContractUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class BlockchainManager<T> {
    private Address admin;
    private SimpleWorld world;
    private MergedContract mergedContract;
    private ArrayList<Block> blockchain;

    public BlockchainManager() {
        this.admin = ContractUtils.generateAddressFromId(Config.ADMIN_ID);
        this.world = new SimpleWorld();
        this.mergedContract = new MergedContract(world, admin);
        this.blockchain = new ArrayList<>();

    }

    public ClientResp handleDecidedValue(T value) {
        // cast the valuse to ClientReq
        if (value == null) {
            Logger.LOG("No decided value");
            return new ClientResp(false, null); //TODO: add message
        }

        // Send answer to clients
        ClientReq decided = (ClientReq) value;
        ClientReqType decidedType = decided.getReqType();

        switch (decidedType) {
            case TRANSFER:
                TransferReq transferReq = (TransferReq) decided;
                return handleTransfer(transferReq);
            case TRANSFER_FROM:
                TransferFromReq transferFromReq = (TransferFromReq) decided;
                return handleTransferFrom(transferFromReq);
            case BLACKLIST:
                BlacklistReq blacklistReq = (BlacklistReq) decided;
                return handleBlacklist(blacklistReq);
            case APPROVE:
                ApproveReq approveReq = (ApproveReq) decided;
                return handleApprove(approveReq);
            default:
                System.out.println("Invalid request type");
                return new ClientResp(false, null);
        }
    }

    public ClientResp handleTransfer(TransferReq req) {
        // empty for now
        return new ClientResp(false, null);
    }

    public ClientResp handleTransferFrom(TransferFromReq req) {
        // empty for now
        return new ClientResp(false, null);
    }

    public ClientResp handleBlacklist(BlacklistReq req) {
        // empty for now
        return new ClientResp(false, null);
    }

    public ClientResp handleApprove(ApproveReq req) {
        // empty for now
        return new ClientResp(false, null);
    }

}
