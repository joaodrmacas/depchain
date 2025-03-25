package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.NodeMessageHandler;
import pt.tecnico.ulisboa.network.message.FragmentedMessage;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.ObservedResource;
import pt.tecnico.ulisboa.utils.RequiresEquals;
import pt.tecnico.ulisboa.utils.SerializationUtils;
public class ClientAplManager<T extends RequiresEquals> extends AplManager {
 
    private ObservedResource<Queue<T>> txQueue;
    private ConcurrentHashMap<Integer, PublicKey> clientKus;

    public ClientAplManager(String address, Integer port, PrivateKey privateKey, ObservedResource<Queue<T>> txQueue,
            ConcurrentHashMap<Integer, PublicKey> clientKus) throws SocketException, IOException {
        super(address, port, privateKey);
        this.txQueue = txQueue;
        this.clientKus = clientKus;
        Logger.LOG("ClientAplManager: " + address + ":" + port);
    }

    @Override
    protected void handleUnknownSender(DatagramPacket packet) {

        Logger.LOG("Creating an apl instance for the new unknown sender.");
        try {
            // KeyMessage message = (KeyMessage) Message.deserialize(packet.getData());

            NodeMessageHandler<T> handler = new NodeMessageHandler<>(txQueue, clientKus);

            FragmentedMessage frag = SerializationUtils.deserializeObject(packet.getData());

            int destId = packet.getPort() - Config.DEFAULT_CLIENT_PORT; //TODO: meti isto para amanha dar para testar com varios clientes sem tar hardcode. Amanha é wild ahahahah
            APLImpl apl = createAPL(destId, packet.getAddress().getHostAddress(), packet.getPort(), handler);
            apl.processReceivedPacket(destId, frag.getFragmentData());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
