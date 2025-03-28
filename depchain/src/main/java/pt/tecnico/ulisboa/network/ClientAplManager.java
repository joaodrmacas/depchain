package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.ServerMessageHandler;
import pt.tecnico.ulisboa.network.message.FragmentedMessage;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.types.Logger;
import pt.tecnico.ulisboa.utils.types.ObservedResource;
import pt.tecnico.ulisboa.utils.types.RequiresEquals;
import pt.tecnico.ulisboa.Account;

public class ClientAplManager<T extends RequiresEquals> extends AplManager {

    private ObservedResource<Queue<T>> txQueue;
    private ConcurrentHashMap<Integer, PublicKey> clientKus;
    private ConcurrentHashMap<Integer, Account> accounts;

    public ClientAplManager(String address, Integer port, PrivateKey privateKey, ObservedResource<Queue<T>> txQueue,
            ConcurrentHashMap<Integer, PublicKey> clientKus, ConcurrentHashMap<Integer, Account> accounts)
            throws SocketException, IOException {
        super(address, port, privateKey);
        this.txQueue = txQueue;
        this.clientKus = clientKus;
        this.accounts = accounts;
        Logger.LOG("ClientAplManager: " + address + ":" + port);
    }

    @Override
    protected void handleUnknownSender(DatagramPacket packet) {

        Logger.LOG("Creating an apl instance for the new unknown sender.");
        try {
            // KeyMessage message = (KeyMessage) Message.deserialize(packet.getData());

            ServerMessageHandler<T> handler = new ServerMessageHandler<>(txQueue, clientKus, accounts);

            FragmentedMessage frag = SerializationUtils.deserializeObject(packet.getData());

            int destId = packet.getPort() - Config.DEFAULT_CLIENT_PORT; // TODO: meti isto para amanha dar para testar
                                                                        // com varios clientes sem tar hardcode. Amanha
                                                                        // é wild
            APLImpl apl = createAPL(destId, packet.getAddress().getHostAddress(), packet.getPort(), handler);
            apl.processReceivedPacket(destId, frag.getFragmentData());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
