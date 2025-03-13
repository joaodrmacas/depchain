package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import pt.tecnico.ulisboa.NodeMessageHandler;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.protocol.RegisterReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.ObservedResource;
import pt.tecnico.ulisboa.utils.RequiresEquals;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public class ClientAplManager<T extends RequiresEquals> extends AplManager {

    private ObservedResource<Queue<T>> txQueue;
    private ConcurrentHashMap<Integer, PublicKey> clientKus;
    private AtomicInteger clientCounter = new AtomicInteger(1);

    public ClientAplManager(String address, Integer port, PrivateKey privateKey, ObservedResource<Queue<T>> txQueue, ConcurrentHashMap<Integer, PublicKey> clientKus ) throws SocketException, IOException {
        super(address, port, privateKey);
        Logger.LOG("ClientAplManager: " + address + ":" + port);
    }

    public ClientAplManager(String address, Integer port) throws SocketException, IOException {
    //TODO: isto está bem?
        this(address, port, null, null, null); 
    }

    @Override
    protected void handleUnknownSender(DatagramPacket packet){
        Logger.LOG("Unknown sender: " + packet.getAddress().getHostAddress() + ":" + packet.getPort());

        int clientid = this.clientCounter.getAndIncrement();

        try {
        RegisterReq msg = (RegisterReq) SerializationUtils.deserializeObject(packet.getData());
        

        NodeMessageHandler<BlockchainMessage> handler = new NodeMessageHandler(clientid,txQueue,clientKus);
        
        PublicKey ku = CryptoUtils.bytesToPublicKey(msg.getKey());
        clientKus.put(id, ku);

        createAPL(id, packet.getAddress().getHostAddress(), packet.getPort(), ku, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
