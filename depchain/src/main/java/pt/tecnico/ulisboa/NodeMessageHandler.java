package pt.tecnico.ulisboa;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.protocol.AppendReq;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.protocol.BlockchainMessage.BlockchainMessageType;
import pt.tecnico.ulisboa.protocol.KeyRegisterReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.RequiresEquals;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public class NodeMessageHandler<T extends RequiresEquals> implements MessageHandler {
    private ConcurrentLinkedQueue<T> txQueue;
    private ConcurrentHashMap<Integer, PublicKey> clientKeys;
    
    public NodeMessageHandler(ConcurrentLinkedQueue<T> txQueue, ConcurrentHashMap<Integer, PublicKey> clientKeys) {
        this.txQueue = txQueue;
        this.clientKeys = clientKeys;
    }

    public void onMessage(int senderId, byte[] message) {

        try {
            BlockchainMessage blockchainMessage = (BlockchainMessage) SerializationUtils.deserializeObject(message);
            BlockchainMessageType type = blockchainMessage.getType();
            switch (type) {
                case APPEND_REQ:
                    //TODO: usar T é disgusting para depois termos este lixo aqui mas ok. Já disse que adoro Generics? -massas 
                    AppendReq<?> appReq = (AppendReq<?>) blockchainMessage;
                    handleAppendRequest(appReq);
                    break;
                case KEY_REGISTER_REQ:
                    KeyRegisterReq keyReq = (KeyRegisterReq) blockchainMessage;
                    handleKeyRegisterRequest(senderId, keyReq);
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
        txQueue.add((T) message);
    }

    public void handleKeyRegisterRequest(int senderId, KeyRegisterReq message) {
        clientKeys.put(senderId, CryptoUtils.bytesToPublicKey(message.getKey()));
    }
}