package pt.tecnico.ulisboa;

import java.security.PublicKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.protocol.AppendReq;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.protocol.BlockchainMessage.BlockchainMessageType;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.ObservedResource;
import pt.tecnico.ulisboa.utils.RequiresEquals;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public class NodeMessageHandler<T extends RequiresEquals> implements MessageHandler {
    private ObservedResource<Queue<T>> txQueue;
    private ConcurrentHashMap<Integer, PublicKey> clientKeys;
    
    public NodeMessageHandler(ObservedResource<Queue<T>> txQueue, ConcurrentHashMap<Integer, PublicKey> clientKeys) {
        this.txQueue = txQueue;
        this.clientKeys = clientKeys;
    }

    public void onMessage(byte[] message) {

        try {
            BlockchainMessage blockchainMessage = (BlockchainMessage) SerializationUtils.deserializeObject(message);
            BlockchainMessageType type = blockchainMessage.getType();
            switch (type) {
                case APPEND_REQ:
                    //TODO: usar T é disgusting para depois termos este lixo aqui mas ok. Já disse que adoro Generics? -massas 
                    AppendReq<?> appReq = (AppendReq<?>) blockchainMessage;
                    handleAppendRequest(appReq);
                    break;
                default:
                    Logger.LOG("Unknown message type: " + type);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    @SuppressWarnings("unchecked")
    public void handleAppendRequest(AppendReq<?> message) {
        String dataToValidate = message.getId() + message.getMessage().toString();
        PublicKey clientKU = clientKeys.get(message.getId());
        if (clientKU == null) {
            Logger.LOG("Client key not found for id: " + message.getId());
            return;
        }
        if (!CryptoUtils.verifySignature(dataToValidate, message.getSignature(), clientKU)) {
            Logger.LOG("Invalid signature for message: " + message);
            return;
        }
        txQueue.getResource().add((T) message);
    }


}