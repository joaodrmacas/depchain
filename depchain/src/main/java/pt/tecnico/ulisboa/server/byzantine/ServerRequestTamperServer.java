package pt.tecnico.ulisboa.server.byzantine;

import java.util.Random;
import java.math.BigInteger;
import java.util.List;

import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.AcceptMessage;
import pt.tecnico.ulisboa.consensus.message.WriteMessage;
import pt.tecnico.ulisboa.consensus.message.ReadMessage;
import pt.tecnico.ulisboa.server.Server;
import pt.tecnico.ulisboa.utils.types.Logger;
import pt.tecnico.ulisboa.Config;


import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.TransferDepCoinReq;
import pt.tecnico.ulisboa.server.Block;

public class ServerRequestTamperServer extends Server {
    
    public ServerRequestTamperServer(int nodeId) {
        super(nodeId);
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java MessageFloodingServer <node-id> [keys-directory]");
            System.exit(1);
        }
        
        int nodeId = Integer.parseInt(args[0]);
        
        try {
            ServerRequestTamperServer node = new ServerRequestTamperServer(nodeId);
            
            if (args.length >= 2) {
                node.setKeysDirectory(args[1]);
            }
            
            node.setup(
                pt.tecnico.ulisboa.utils.GeneralUtils.id2Addr.get(nodeId),
                pt.tecnico.ulisboa.utils.GeneralUtils.id2ClientPort.get(nodeId),
                pt.tecnico.ulisboa.utils.GeneralUtils.id2ServerPort.get(nodeId)
            );
            node.mainLoop();
            
        } catch (Exception e) {
            Logger.ERROR("Node setup failed", e);
        }
    }


    @Override
    public void sendToMember(int memberId, ConsensusMessage msg) {

        if (msg instanceof WriteMessage || msg instanceof AcceptMessage) {
            if (msg instanceof WriteMessage){
                WriteMessage writeMsg = (WriteMessage) msg;
                Block originalBlock = (Block) writeMsg.getValue();
                List<ClientReq> transactions = originalBlock.getTransactions();
    
                // Modify the last transaction in the block to send myself money
                String addr = Config.CLIENT_ID_2_ADDR.get(getId());
                TransferDepCoinReq transferReq = new TransferDepCoinReq(-2, Long.valueOf(3), addr, BigInteger.valueOf(100));
                //dont change signature
                transactions.set(transactions.size()-1, transferReq);
                writeMsg.setValue(originalBlock);
            } else {
                AcceptMessage acceptMsg = (AcceptMessage) msg;
                Block originalBlock = (Block) acceptMsg.getValue();
                List<ClientReq> transactions = originalBlock.getTransactions();
                // Modify the last transaction in the block to send myself money
                String addr = Config.CLIENT_ID_2_ADDR.get(getId());
                TransferDepCoinReq transferReq = new TransferDepCoinReq(-2, Long.valueOf(3), addr, BigInteger.valueOf(100));
                transactions.set(transactions.size()-1, transferReq);
                acceptMsg.setValue(originalBlock);
            }
    
        }
        super.sendToMember(memberId, msg);
    }
}