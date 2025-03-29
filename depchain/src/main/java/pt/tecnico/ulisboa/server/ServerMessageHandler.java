package pt.tecnico.ulisboa.server;

import java.security.PublicKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.BlockchainMessage.BlockchainMessageType;
import pt.tecnico.ulisboa.protocol.RegisterReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.types.Logger;
import pt.tecnico.ulisboa.utils.types.ObservedResource;
import pt.tecnico.ulisboa.utils.types.RequiresEquals;
import pt.tecnico.ulisboa.utils.ContractUtils;
import java.util.Random;

public class ServerMessageHandler<T extends RequiresEquals> implements MessageHandler {
    private ObservedResource<Queue<T>> txQueue;
    private ConcurrentHashMap<Integer, PublicKey> clientKus;
    private ConcurrentHashMap<Integer, Account> clientAccounts;

    public ServerMessageHandler(ObservedResource<Queue<T>> txQueue, ConcurrentHashMap<Integer, PublicKey> clientKus, ConcurrentHashMap<Integer, Account> clientAccounts) {
        this.clientKus = clientKus;
        this.clientAccounts = clientAccounts;
        this.txQueue = txQueue;
    }

    public void onMessage(int senderid, byte[] message) {
        try {
            BlockchainMessage blockchainMessage = (BlockchainMessage) SerializationUtils.deserializeObject(message);
            BlockchainMessageType type = blockchainMessage.getType();
            switch (type) {
                case REGISTER_REQ:
                    RegisterReq keyReq = (RegisterReq) blockchainMessage;
                    int senderId = keyReq.getSenderId();
                    handleRegisterRequest(senderId, keyReq);
                    break;
                case CLIENT_REQ:
                    handleClientRequest((ClientReq) blockchainMessage);
                    break;
                default:
                    Logger.LOG("Unknown message type: " + type);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleRegisterRequest(int senderId, RegisterReq message) {
        PublicKey ku = CryptoUtils.bytesToPublicKey(message.getKey());
        if (clientKus.containsKey(senderId)) {
            Logger.LOG("Client already registered");
            return;
        }
        clientKus.put(senderId, ku);
        
        //TODO: change this? genesis block? what defines admin account balance?
        Random random = new Random();
        int randomNumber = random.nextInt(100);
        clientAccounts.put(senderId, new Account(ContractUtils.generateAddressFromId(senderId), randomNumber));
    }

    @SuppressWarnings("unchecked")
    public void handleClientRequest(ClientReq message) {
        PublicKey clientKU = clientKus.get(message.getSenderId());
        if (clientKU == null) {
            Logger.LOG("Client key not found for id: " + message.getSenderId());
            return;
        }
        if (!CryptoUtils.verifySignature(message.toString(), message.getSignature(), clientKU)) {
            Logger.LOG("Invalid signature for message: " + message);
            return;
        }
        Logger.LOG("Valid signature for message: " + message.getCount());
        txQueue.getResource().add((T) message);
        txQueue.notifyChange();
    }
}