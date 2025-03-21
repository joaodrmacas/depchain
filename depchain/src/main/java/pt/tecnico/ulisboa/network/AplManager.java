package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.message.FragmentedMessage;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public abstract class AplManager {
    private final DatagramSocket socket;

    private final String nodeAddress;
    private final Integer nodePort;
    private final ConcurrentHashMap<Integer, APLImpl> aplInstances = new ConcurrentHashMap<>();
    protected final Map<String, Integer> senderIdMap = new ConcurrentHashMap<>();
    private final PrivateKey privateKey;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Map<Integer, Map<Long, List<FragmentedMessage>>> fragmentBuffer = new ConcurrentHashMap<>();

    public AplManager(String address, Integer port, PrivateKey privateKey) throws SocketException, IOException {
        this.privateKey = privateKey;
        this.nodeAddress = address;
        this.nodePort = port;

        Logger.LOG("Creating shared socket - IP: " + address + " Port: " + port);
        this.socket = new DatagramSocket(port, InetAddress.getByName(address));
    }

    public AplManager(String address, Integer port) throws SocketException, IOException {
        this(address, port, null);
    }

    public APLImpl createAPL(Integer destId, String destAddress, Integer destPort, PublicKey destPublicKey,
            MessageHandler msgHandler) throws Exception {
        if (aplInstances.containsKey(destId)) {
            Logger.LOG("APL instance already exists for destination ID: " + destId + ". Returning existing instance.");
            return aplInstances.get(destId);
        }

        APLImpl apl = new APLImpl(nodeAddress, nodePort, destAddress, destPort, privateKey, destPublicKey, socket,
                msgHandler);
        aplInstances.put(destId, apl);
        senderIdMap.put(destAddress + ":" + destPort, destId);

        return apl;
    }

    public APLImpl createAPL(Integer destId, String destAddress, Integer destPort,
            MessageHandler msgHandler) throws Exception {
        if (aplInstances.containsKey(destId)) {
            Logger.LOG("APL instance already exists for destination ID: " + destId + ". Returning existing instance.");
            return aplInstances.get(destId);
        }

        APLImpl apl = new APLImpl(nodeAddress, nodePort, destAddress, destPort, privateKey, socket, msgHandler);
        aplInstances.put(destId, apl);
        senderIdMap.put(destAddress + ":" + destPort, destId);

        return apl;
    }

    public APLImpl getAPL(int destId) {
        return aplInstances.get(destId);
    }

    public Integer getSenderId(String address, Integer port) {
        return senderIdMap.get(address + ":" + port);
    }

    public void removeAPL(int destId) {
        aplInstances.remove(destId);
        senderIdMap.entrySet().removeIf(entry -> entry.getValue().equals(destId));
    }

    public void send(int destId, byte[] message) {
        APLImpl apl = aplInstances.get(destId);
        if (apl != null) {
            apl.send(message);
        } else {
            Logger.LOG(
                    "No APL instance found for destination ID: " + destId + " when trying to send message: " + message);
            // print known senders
            Logger.LOG("Known senders:");
            for (Integer key : aplInstances.keySet()) {
                Logger.LOG("Known sender: " + key);
            }
        }
    }

    public void send(int destId, Serializable message) {
        APLImpl apl = aplInstances.get(destId);
        if (apl != null) {
            apl.send(message);
        } else {
            Logger.LOG(
                    "No APL instance found for destination ID: " + destId + " when trying to send message: " + message);
            // print known senders
            Logger.LOG("Known senders:");
            for (String key : senderIdMap.keySet()) {
                Logger.LOG("Known sender: " + key);
            }
        }
    }

    // This method is used to test the integrity of the message
    public void sendAndTamper(int destId, Serializable message) {
        APLImpl apl = aplInstances.get(destId);
        if (apl != null) {
            apl.sendAndTamper(message);
        } else {
            Logger.LOG(
                    "No APL instance found for destination ID: " + destId + " when trying to send message: " + message);
            // print known senders
            Logger.LOG("Known senders:");
            for (String key : senderIdMap.keySet()) {
                Logger.LOG("Known sender: " + key);
            }
        }
    }

    public void sendWithTimeout(int destId, Serializable message, int timeout) {
        APLImpl apl = aplInstances.get(destId);
        if (apl != null) {
            apl.sendWithTimeout(message, timeout);
        } else {
            Logger.LOG(
                    "No APL instance found for destination ID: " + destId + " when trying to send message: " + message);
            // print known senders
            Logger.LOG("Known senders:");
            for (String key : senderIdMap.keySet()) {
                Logger.LOG("Known sender: " + key);
            }
        }
    }

    // Start the message dispatcher thread
    public void startListening() {
        isRunning.set(true);
        new Thread(() -> {
            byte[] buffer = new byte[Config.BUFFER_SIZE];

            while (isRunning.get()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Determine the sender
                    Integer senderId = getSenderId(packet.getAddress().getHostAddress(), packet.getPort());
                    if (senderId == null) {
                        Logger.LOG(
                                "Received packet from unknown sender: " + packet.getAddress().getHostAddress() + ":"
                                        + packet.getPort() + ".");

                        // print all known senders
                        // Logger.LOG("Known senders:");
                        // for (String key : senderIdMap.keySet()) {
                        // Logger.LOG("Known sender: " + key);
                        // }
                        handleUnknownSender(packet);
                        continue;
                    }

                    Logger.LOG("Received packet from sender ID: " + senderId);

                    //receive fragment
                    FragmentedMessage fragmentedMessage = SerializationUtils.deserializeObject(packet.getData());

                    //how many fragments are there
                    long totalFragments = fragmentedMessage.getTotalFragments();
                    
                    if (totalFragments != 1) {

                        if (!fragmentBuffer.containsKey(senderId)) {
                            fragmentBuffer.put(senderId, new ConcurrentHashMap<>());
                        }
                        fragmentBuffer.get(senderId).putIfAbsent(fragmentedMessage.getMessageId(), [] );

                    }
                    

                    fragmentBuffer.putIfAbsent(senderId, new ConcurrentHashMap<>());

                    // Get the APL for this sender and dispatch the data of the packet
                    APLImpl apl = aplInstances.get(senderId);
                    if (apl != null) {
                        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                        apl.handleData(senderId, data);
                    } else {
                        Logger.LOG("No APL instance found for sender ID: " + senderId);
                    }

                } catch (Exception e) {
                    if (isRunning.get()) {
                        Logger.ERROR("Error receiving packet", e);
                    }
                }
            }
        }).start();

    }

    protected abstract void handleUnknownSender(DatagramPacket data);

    public void stopListening() {
        isRunning.set(false);
    }

    // Clean shutdown
    public void close() {
        isRunning.set(false);

        // Close all APL instances
        for (APLImpl apl : aplInstances.values()) {
            apl.close();
        }

        aplInstances.clear();

        // Close the socket
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
