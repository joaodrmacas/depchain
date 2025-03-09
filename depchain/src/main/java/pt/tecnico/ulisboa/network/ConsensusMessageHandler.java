package pt.tecnico.ulisboa.network;

import java.io.IOException;

import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.consensus.message.AcceptMessage;
import pt.tecnico.ulisboa.consensus.message.CollectedMessage;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.ReadMessage;
import pt.tecnico.ulisboa.consensus.message.StateMessage;
import pt.tecnico.ulisboa.consensus.message.WriteMessage;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.consensus.WriteTuple;

public class ConsensusMessageHandler implements MessageHandler {
    
    @Override
    public void onMessage(int senderId, byte[] message) {
        try {
            Object deserializedObject = SerializationUtils.deserializeObject(message);
            if (deserializedObject instanceof ConsensusMessage<?>) {
                ConsensusMessage<?> consensusMessage = (ConsensusMessage<?>) deserializedObject;
                
                Logger.LOG("Received consensus message of type: " + consensusMessage.getType() + " from node: " + senderId);
                
                //TODO: Como dar delivar para a app? Para já deixei assim
                // Podemos passar para dentro deste handler o um objeto do consensus que ao receber a mensagem faz x cena (ex: registar que mais um processo devolveu a mensagem de read). -Massas  
                switch (consensusMessage.getType()) {
                    case READ:
                        handleReadMessage(senderId, (ReadMessage<?>) consensusMessage);
                        break;
                    case STATE:
                        handleStateMessage(senderId, (StateMessage<?>) consensusMessage);
                        break;
                    case WRITE:
                        handleWriteMessage(senderId, (WriteMessage<?>) consensusMessage);
                        break;
                    case ACCEPT:
                        handleAcceptMessage(senderId, (AcceptMessage<?>) consensusMessage);
                        break;
                    case COLLECTED:
                        handleCollectedMessage(senderId, (CollectedMessage<?>) consensusMessage);  
                        break;
                    default:
                        Logger.LOG("Unknown consensus message type: " + consensusMessage.getType());
                }
            } 
        } catch (IOException | ClassNotFoundException e) {
            Logger.LOG("Failed to deserialize message in onMessage: " + e.getMessage());
        }
    }

    private <T> void handleReadMessage(int senderId, ReadMessage<T> readMessage) {
        // Handle READ message
        System.out.println("Received READ message from node: " + senderId);
    }

    private <T> void handleStateMessage(int senderId, StateMessage<T> stateMessage) {
        // Handle STATE message
        System.out.println("Received STATE message from node: " + senderId);
    }

    private <T> void handleWriteMessage(int senderId, WriteMessage<T> writeMessage) {
        // Handle WRITE message
        WriteTuple<T> t = writeMessage.getTuple();
        System.out.println("Received WRITE message from node: " + senderId + " with tuple: " + t);
    }

    private <T> void handleAcceptMessage(int senderId, AcceptMessage<T> acceptMessage) {
        // Handle ACCEPT message
        System.out.println("Received ACCEPT message from node: " + senderId);
    }

    private <T> void handleCollectedMessage(int senderId, CollectedMessage<T> collectedMessage) {
        // Handle COLLECTED message
        System.out.println("Received COLLECTED message from node: " + senderId);
    }

}
