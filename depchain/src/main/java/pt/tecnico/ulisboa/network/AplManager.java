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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.message.FragmentedMessage;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.types.ArrayWithCounter;
import pt.tecnico.ulisboa.utils.types.Logger;

public abstract class AplManager {
    private DatagramSocket socket;

    private final String nodeAddress;
    private final Integer nodePort;
    private final ConcurrentHashMap<Integer, APLImpl> aplInstances = new ConcurrentHashMap<>();
    protected final Map<String, Integer> senderIdMap = new ConcurrentHashMap<>();
    private final PrivateKey privateKey;
    private AtomicBoolean isListening = new AtomicBoolean(false);
    private final Map<Integer, Map<String, ArrayWithCounter<FragmentedMessage>>> fragmentBuffers = new ConcurrentHashMap<>();

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

    public void sendToAll(Serializable message) {
        for (APLImpl apl : aplInstances.values()) {
            apl.send(message);
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
        this.isListening.set(true);
        new Thread(() -> {
            byte[] buffer = new byte[Config.BUFFER_SIZE];

            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    if (!isListening()) {
                        Logger.LOG("Not listening, dropping packet.");
                        continue;
                    }
                    // Determine the sender
                    Integer senderId = getSenderId(packet.getAddress().getHostAddress(), packet.getPort());
                    if (senderId == null) {
                        Logger.LOG(
                                "Received packet from unknown sender: " + packet.getAddress().getHostAddress() + ":"
                                        + packet.getPort() + ".");
                        handleUnknownSender(packet);
                        continue;
                    }

                    // Logger.LOG("Received packet from sender ID: " + senderId);

                    // receive fragment
                    byte[] actualData = Arrays.copyOf(packet.getData(), packet.getLength());
                    FragmentedMessage fragmentedMessage = SerializationUtils.deserializeObject(actualData);

                    // how many fragments are there
                    int totalFragments = fragmentedMessage.getTotalFragments();

                    if (!fragmentBuffers.containsKey(senderId)) {
                        fragmentBuffers.put(senderId, new ConcurrentHashMap<>());
                    }

                    fragmentBuffers.get(senderId).putIfAbsent(fragmentedMessage.getMessageId(),
                            new ArrayWithCounter<FragmentedMessage>(totalFragments));
                    ArrayWithCounter<FragmentedMessage> fragmentsArray = fragmentBuffers.get(senderId)
                            .get(fragmentedMessage.getMessageId());
                    fragmentsArray.put(fragmentedMessage, fragmentedMessage.getFragmentIndex());
                    Object[] objectArray = fragmentsArray.getIfFullAndReset();
                    if (objectArray == null) {
                        continue;
                    }

                    FragmentedMessage[] frags = new FragmentedMessage[objectArray.length];
                    for (int i = 0; i < objectArray.length; i++) {
                        frags[i] = (FragmentedMessage) objectArray[i];
                    }

                    byte[] data = FragmentedMessage.reassembleFragments(frags);

                    // Logger.LOG("Original message size: " + frags[0].getOriginalMessageSize());
                    // Logger.LOG("Reassembled message size: " + data.length);

                    // Get the APL for this sender and dispatch the data of the packet
                    APLImpl apl = aplInstances.get(senderId);
                    if (apl != null) {
                        apl.handleMessage(senderId, data);
                    } else {
                        Logger.LOG("No APL instance found for sender ID: " + senderId);
                    }

                }
                // catch socket is closed exection
                catch (SocketException e) {
                    if (socket.isClosed()) {
                        Logger.LOG("Socket closed, stopping listener.");
                        break;
                    }
                    Logger.ERROR("Socket exception: " + e.getMessage());
                } catch (Exception e) {
                    Logger.LOG("Error receiving packet" + e);
                }
            }
        }).start();

    }

    protected abstract void handleUnknownSender(DatagramPacket data);

    public boolean isListening() {
        return this.isListening.get();
    }

    // this is only used for testing purposes
    public void setListening(boolean listening) {
        this.isListening.set(listening);
        Logger.LOG("AplManager is now " + (listening ? "" : "Not ") + "listening.");
    }

    // Clean shutdown
    public void close() {
        // Close all APL instances
        for (APLImpl apl : aplInstances.values()) {
            apl.close();
        }

        aplInstances.clear();
        try {
            Thread.sleep(500); // Wait for threads to finish
        } catch (InterruptedException e) {
            Logger.ERROR("Thread sleep interrupted", e);
            Thread.currentThread().interrupt(); // Restore the interrupted status
        }

        // Close the socket
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
