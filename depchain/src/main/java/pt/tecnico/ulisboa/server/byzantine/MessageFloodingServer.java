package pt.tecnico.ulisboa.server.byzantine;

import java.util.Random;

import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.AcceptMessage;
import pt.tecnico.ulisboa.consensus.message.WriteMessage;
import pt.tecnico.ulisboa.consensus.message.ReadMessage;
import pt.tecnico.ulisboa.server.Server;
import pt.tecnico.ulisboa.utils.types.Logger;

public class MessageFloodingServer extends Server {
    private final Random random = new Random();
    
    public MessageFloodingServer(int nodeId) {
        super(nodeId);
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java MessageFloodingServer <node-id> [keys-directory]");
            System.exit(1);
        }
        
        int nodeId = Integer.parseInt(args[0]);
        
        try {
            MessageFloodingServer node = new MessageFloodingServer(nodeId);
            
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
        // First send the original message
        super.sendToMember(memberId, msg);
        
        // Determine whether to flood for this message type
        boolean shouldFlood = false;
        int numDuplicates = 0;
        
        if (msg instanceof ReadMessage) {
            shouldFlood = true;
            numDuplicates = 2 + random.nextInt(3); // 2-4 duplicates
            Logger.LOG("Flooding READ messages to member " + memberId + " with " + numDuplicates + " duplicates");
        }
        else if (msg instanceof WriteMessage) {
            shouldFlood = true;
            numDuplicates = 3 + random.nextInt(5); // 3-7 duplicates
            Logger.LOG("Flooding WRITE messages to member " + memberId + " with " + numDuplicates + " duplicates");
        }
        else if (msg instanceof AcceptMessage) {
            shouldFlood = true;
            numDuplicates = 5 + random.nextInt(5); // 5-9 duplicates
            Logger.LOG("Flooding ACCEPT messages to member " + memberId + " with " + numDuplicates + " duplicates");
        }
        
        // Send duplicate messages if needed
        if (shouldFlood) {
            for (int i = 0; i < numDuplicates; i++) {
                // Small delay between messages to ensure they don't all arrive at exactly the same time
                try {
                    Thread.sleep(10 + random.nextInt(20));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Send a duplicate of the original message
                super.sendToMember(memberId, msg);
            }
        }
    }
}