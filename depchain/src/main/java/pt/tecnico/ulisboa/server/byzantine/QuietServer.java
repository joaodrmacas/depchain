package pt.tecnico.ulisboa.server.byzantine;

import pt.tecnico.ulisboa.server.Server;
import pt.tecnico.ulisboa.utils.GeneralUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class QuietServer extends Server {

    public QuietServer(int nodeId) {
        super(nodeId);
    }
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Node <node-id> [keys-directory]");
            System.exit(1);
        }

        int nodeId = Integer.parseInt(args[0]);

        String address = GeneralUtils.id2Addr.get(nodeId);
        int clientPort = GeneralUtils.id2ClientPort.get(nodeId);
        int serverPort = GeneralUtils.id2ServerPort.get(nodeId);

        try {
            Server node = new QuietServer(nodeId);

            if (args.length >= 2) {
                node.setKeysDirectory(args[1]);
            }

            node.setup(address, clientPort, serverPort);
            node.mainLoop();

        } catch (Exception e) {
            Logger.ERROR("Node setup failed", e);
        }
    }

    @Override
    public void mainLoop() {
        //This loop is responsible for starting the threads that handle the consensus messages,
        //the execution of transactions and the handler of decided blocks.
        //This mimics the server belonging to the consensus group (receives the messages but doesnt handle them)
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
