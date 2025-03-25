package pt.tecnico.ulisboa;

import java.security.PublicKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.AppendReq;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.protocol.BlockchainMessage.BlockchainMessageType;
import pt.tecnico.ulisboa.protocol.ClientReq.ClientReqType;
import pt.tecnico.ulisboa.protocol.RegisterReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.ObservedResource;
import pt.tecnico.ulisboa.utils.RequiresEquals;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public class NodeMessageHandler<T extends RequiresEquals> implements MessageHandler {
    private ObservedResource<Queue<T>> txQueue;
    private ConcurrentHashMap<Integer, PublicKey> clientKus;

    public NodeMessageHandler(ObservedResource<Queue<T>> txQueue, ConcurrentHashMap<Integer, PublicKey> clientKus) {
        this.clientKus = clientKus;
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
                    // TODO: assuming its a AppendReq for now
                    ClientReq clientReq = (ClientReq) blockchainMessage;
                    handleClientReq(clientReq);
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
    }

    @SuppressWarnings("unchecked") // TODO: this is a hacky way to avoid unchecked cast warning
    public void handleClientReq(ClientReq request) {

        ClientReqType type = request.getReqType();

        // TODO: assuming its an append request for now
        if (type != ClientReqType.APPEND_REQ) {
            Logger.LOG("Unknown request type: " + type);
            return;
        }

        // TODO: assuming its an append request. type cheking should even be needed here tho
        AppendReq<?> appendReq = (AppendReq<?>) request;

        // TODO: this dataToValidate should be the toString of the request. This way, we
        // could validate the signature for any request without having to check for its
        // reqType (knid of hacky tho idk
        String dataToValidate = appendReq.getId().toString() + appendReq.getMessage().toString()
                + appendReq.getCount().toString();
        PublicKey clientKU = clientKus.get(appendReq.getId());
        if (clientKU == null) {
            Logger.LOG("Client key not found for id: " + appendReq.getId());
            return;
        }
        if (!CryptoUtils.verifySignature(dataToValidate, appendReq.getSignature(), clientKU)) {
            Logger.LOG("Invalid signature for appendReq: " + appendReq);
            return;
        }
        Logger.LOG("Valid signature for appendReq: " + appendReq.getCount());

        txQueue.getResource().add((T) appendReq);
        txQueue.notifyChange();

    }
}