package pt.tecnico.ulisboa.server.byzantine;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.server.Server;
import pt.tecnico.ulisboa.utils.types.Logger;

public class DelayedServer extends Server {
    private final Random random = new Random();
    private final int maxDelayMs = 3000;
    
    public DelayedServer(int nodeId) {
        super(nodeId);
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java DelayedServer <node-id> [keys-directory]");
            System.exit(1);
        }
        
        int nodeId = Integer.parseInt(args[0]);
        
        try {
            DelayedServer node = new DelayedServer(nodeId);
            
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
        try {
            int delay = random.nextInt(maxDelayMs);
            Logger.LOG("Introducing " + delay + "ms delay before sending to member " + memberId);
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        super.sendToMember(memberId, msg);
    }
}