package pt.tecnico.ulisboa.server.byzantine;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage.MessageType;
import pt.tecnico.ulisboa.server.Server;
import pt.tecnico.ulisboa.utils.types.Logger;

public class SelectiveDropServer extends Server {
    private final Random random = new Random();
    private final Set<Integer> targetedNodes = new HashSet<>();
    private final Set<MessageType> targetedMessageTypes = new HashSet<>();
    
    public SelectiveDropServer(int nodeId) {
        super(nodeId);
        
        // Select random nodes to target
        int numNodesToTarget = 1 + random.nextInt(2);
        while (targetedNodes.size() < numNodesToTarget) {
            int targetNodeId = random.nextInt(pt.tecnico.ulisboa.Config.NUM_MEMBERS);
            if (targetNodeId != nodeId) {
                targetedNodes.add(targetNodeId);
            }
        }
        
        // Select specific message types to target
        targetedMessageTypes.add(MessageType.WRITE);
        targetedMessageTypes.add(MessageType.ACCEPT);
        
        Logger.LOG("Selectively targeting nodes: " + targetedNodes);
        Logger.LOG("Selectively targeting message types: " + targetedMessageTypes);
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java SelectiveDropServer <node-id> [keys-directory]");
            System.exit(1);
        }
        
        int nodeId = Integer.parseInt(args[0]);
        
        try {
            SelectiveDropServer node = new SelectiveDropServer(nodeId);
            
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
        if (targetedNodes.contains(memberId) && targetedMessageTypes.contains(msg.getType())) {
            if (random.nextDouble() < 0.8) { // 80% chance to drop
                Logger.LOG("Selectively dropping " + msg.getType() + " message to targeted node " + memberId);
                return; // Drop the message
            }
        }
        
        super.sendToMember(memberId, msg);
    }

}