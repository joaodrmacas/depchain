package pt.tecnico.ulisboa.server.byzantine;

import java.util.Random;

import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.TransferDepCoinReq;
import pt.tecnico.ulisboa.server.Server;
import pt.tecnico.ulisboa.utils.types.Logger;

public class ClientRequestTamperServer extends Server {
    private final Random random = new Random();

    public ClientRequestTamperServer(int nodeId) {
        super(nodeId);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ClientRequestTamperServer <node-id> [keys-directory]");
            System.exit(1);
        }

        int nodeId = Integer.parseInt(args[0]);

        try {
            ClientRequestTamperServer node = new ClientRequestTamperServer(nodeId);

            if (args.length >= 2) {
                node.setKeysDirectory(args[1]);
            }

            node.setup(
                    pt.tecnico.ulisboa.utils.GeneralUtils.id2Addr.get(nodeId),
                    pt.tecnico.ulisboa.utils.GeneralUtils.id2ClientPort.get(nodeId),
                    pt.tecnico.ulisboa.utils.GeneralUtils.id2ServerPort.get(nodeId));
            node.mainLoop();

        } catch (Exception e) {
            Logger.ERROR("Node setup failed", e);
        }
    }

    @Override
    public void handleClientRequest(ClientReq tx) {
        // Target a specifc person

        Logger.LOG("Handling client request: " + tx.toString());

        if (tx.getSenderId() == -1) {
            try {
                if (tx instanceof TransferDepCoinReq) {
                    TransferDepCoinReq transferReq = (TransferDepCoinReq) tx;
                    
                    Logger.LOG("Tampering with transfer request, changing receiver address");
                    
                    // Modify receiver address 
                    transferReq.setReceiver("0x0000000000000000000000000000000000000002");

                    tx = transferReq; // Update reference to tampered request
                } 
            } catch (Exception e) {
                Logger.ERROR("Exception while tampering with client request", e);
            }
        }
        super.handleClientRequest(tx);
    }
}