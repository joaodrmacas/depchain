package pt.tecnico.ulisboa.server.byzantine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.AcceptMessage;
import pt.tecnico.ulisboa.consensus.message.WriteMessage;
import pt.tecnico.ulisboa.server.Server;
import pt.tecnico.ulisboa.utils.types.Logger;

public class MessageReplayServer extends Server {
    private final Random random = new Random();
    private final Map<Integer, List<ConsensusMessage>> recordedMessages = new HashMap<>();
    
    public MessageReplayServer(int nodeId) {
        super(nodeId);
        // Initialize message history for each member
        for (int i = 0; i < pt.tecnico.ulisboa.Config.NUM_MEMBERS; i++) {
            if (i != nodeId) {
                recordedMessages.put(i, new ArrayList<>());
            }
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java MessageReplayServer <node-id> [keys-directory]");
            System.exit(1);
        }
        
        int nodeId = Integer.parseInt(args[0]);
        
        try {
            MessageReplayServer node = new MessageReplayServer(nodeId);
            
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
        
        // Record important messages for future replay
        if (msg instanceof WriteMessage || msg instanceof AcceptMessage) {
            try {
                // Deep copy the message by serializing and deserializing
                byte[] serialized = pt.tecnico.ulisboa.utils.SerializationUtils.serializeObject(msg);
                ConsensusMessage copy = (ConsensusMessage) pt.tecnico.ulisboa.utils.SerializationUtils.deserializeObject(serialized);
                
                recordedMessages.get(memberId).add(copy);
                Logger.LOG("Recorded " + msg.getType() + " message for potential replay");
                
                // Keep the last 10 messages per member
                if (recordedMessages.get(memberId).size() > 10) {
                    recordedMessages.get(memberId).remove(0);
                }
            } catch (Exception e) {
                Logger.ERROR("Failed to record message", e);
            }
        }
        
        // Randomly replay old messages
        if (!recordedMessages.get(memberId).isEmpty()) {
            int index = random.nextInt(recordedMessages.get(memberId).size());
            ConsensusMessage oldMsg = recordedMessages.get(memberId).get(index);
            
            Logger.LOG("Replaying old " + oldMsg.getType() + " message from epoch " + 
                      oldMsg.getEpochNumber() + " to member " + memberId);
            
            // Add some delay before replaying to make it more disruptive
            try {
                Thread.sleep(100 + random.nextInt(200));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            super.sendToMember(memberId, oldMsg);
        }
        super.sendToMember(memberId, msg);
        super.sendToMember(memberId, msg);
    }
}