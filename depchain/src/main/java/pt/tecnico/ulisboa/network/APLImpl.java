package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.SecretKey;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.network.message.AckMessage;
import pt.tecnico.ulisboa.network.message.DataMessage;
import pt.tecnico.ulisboa.network.message.KeyMessage;
import pt.tecnico.ulisboa.network.message.Message;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.GeneralUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public class APLImpl implements APL {
    private MessageHandler messageHandler;
    private final DatagramSocket socket;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final ConcurrentHashMap<Long, Message> pendingMessages = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> receivedMessages = new ConcurrentHashMap<>();

    private long nextSeqNum = 1L;
    private long lastReceivedSeqNum = 1L;

    private final PrivateKey privateKey;
    private final PublicKey destPublicKey;
    private SecretKey secretKey;

    String destAddress;
    Integer destPort;

    // Constructor that accepts an existing socket
    public APLImpl(String destAddress, Integer destPort, PrivateKey privateKey, PublicKey destPublicKey, DatagramSocket socket, MessageHandler messageHandler)
            throws GeneralSecurityException, IOException {
        this.socket = socket;
        this.destAddress = destAddress;
        this.destPort = destPort;
        this.privateKey = privateKey;
        this.destPublicKey = destPublicKey;
        setMessageHandler(messageHandler);

        startRetransmissionScheduler();

        Logger.LOG("One-to-One APL created with node ID: " + " connected to destination: " + destId +
                " using existing socket on port: " + socket.getLocalPort());
    }

    // // For unit tests
    // public APLImpl(int nodeId, int destId, PrivateKey privateKey, PublicKey destPublicKey,
    //         MessageHandler messageHandler)
    //         throws SocketException, GeneralSecurityException, IOException {
    //     this(nodeId, destId, privateKey, destPublicKey);
    //     this.messageHandler = messageHandler;
    // }

    // For unit tests with existing socket
    public APLImpl(String destAddress, Integer destPort, PrivateKey privateKey, PublicKey destPublicKey,
            MessageHandler messageHandler, DatagramSocket socket)
            throws GeneralSecurityException, IOException {
        this(destAddress, destPort, privateKey, destPublicKey, socket, messageHandler);
    }

    public void sendKeyMessage(KeyMessage keyMessage) {
        long seqNum = nextSeqNum++;
        pendingMessages.put(seqNum, keyMessage);
        Logger.LOG("Sending key message: " + keyMessage);
        sendUdpPacket(keyMessage.serialize());
    }

    public void sendDataMessage(DataMessage dataMsg) {
        long seqNum = nextSeqNum++;
        pendingMessages.put(seqNum, dataMsg);
        Logger.LOG("Sending message: " + dataMsg);
        sendUdpPacket(dataMsg.serialize());
    }

    public void send(Serializable obj) {
        byte[] messageBytes = null;
        try {
            messageBytes = SerializationUtils.serializeObject(obj);
        } catch (IOException e) {
            System.err.println("Failed to serialize message: " + e.getMessage());
            return;
        }
        send(messageBytes);
    }

    public void send(byte[] message) {
        SecretKey secretKey = getOrGenerateSecretKey();
        long seqNum = nextSeqNum++;

        try {
            byte[] hmac = generateHMAC(message, seqNum, secretKey);

            DataMessage dataMsg = new DataMessage(message, seqNum, hmac);

            // Store the message in the pending list
            pendingMessages.put(seqNum, dataMsg);

            Logger.LOG("Sending message: " + dataMsg);
            sendUdpPacket(dataMsg.serialize());
        } catch (Exception e) {
            System.err.println("Failed to sign and send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    public boolean handlePacket(DatagramPacket packet) {
        try {
            byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
            String senderaddr = packet.getAddress().getHostAddress() + ":" + packet.getPort();
            int senderId = GeneralUtils.serversAddr2Id.get(senderaddr);

            // Only process messages from our designated destination
            dAddr = destAddress + ":" + destPort;
            if (senderaddr == dAddr ) { // If this fails. there is probably a problem in the manager
                Logger.LOG("Received message from destination: " + destId);
                processReceivedPacket(data);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void processReceivedPacket(byte[] data) {
        Logger.LOG("Processing message from destination");
        Message message = Message.deserialize(data);
        if (message == null)
            return;

        if (message.getSeqNum() != lastReceivedSeqNum) {
            Logger.LOG("Received out-of-order message");
            return;
        }

        lastReceivedSeqNum++;

        switch (message.getType()) {
            case Message.DATA_MESSAGE_TYPE:
                DataMessage dataMessage = (DataMessage) message;
                handleData(dataMessage);
                break;
            case Message.ACK_MESSAGE_TYPE:
                AckMessage ackMessage = (AckMessage) message;
                handleACK(ackMessage);
                break;
            case Message.KEY_MESSAGE_TYPE:
                KeyMessage keyMessage = (KeyMessage) message;
                handleKey(keyMessage);
                break;
            default:
                System.err.println("Unknown message type: " + message.getType());
        }
    }

    private void handleKey(KeyMessage keyMessage) {
        Logger.LOG("Received key message");
        SecretKey secretKey = null;
        try {
            secretKey = CryptoUtils.decryptSymmetricKey(keyMessage.getContent(), privateKey);
        } catch (Exception e) {
            System.err.println("Failed to decrypt symmetric key: " + e.getMessage());
            return;
        }

        this.secretKey = secretKey;

        Logger.LOG("Received key: " + secretKey);

        // Send authenticated acknowledgment
        try {
            sendAuthenticatedAcknowledgment(keyMessage.getSeqNum());
        } catch (Exception e) {
            System.err.println("Failed to send acknowledgment: " + e.getMessage());
        }

        // Check for duplicates
        if (isDuplicate(keyMessage.getSeqNum())) {
            return;
        }
    }

    private void handleACK(AckMessage ackMessage) {
        byte[] content = ackMessage.getContent();
        long seqNum = ackMessage.getSeqNum();
        byte[] hmac = ackMessage.getMac();

        if (!verifyHMAC(content, seqNum, hmac)) {
            System.err.println("Authentication failed for ACK");
            return;
        }

        // Remove the message from the pending list
        if (!pendingMessages.containsKey(seqNum)) {
            Logger.LOG("Received ACK for unsent message: " + seqNum);
        }

        pendingMessages.remove(seqNum);

    }

    private void handleData(DataMessage dataMessage) {
        byte[] content = dataMessage.getContent();
        long seqNum = dataMessage.getSeqNum();
        byte[] hmac = dataMessage.getMac();

        if (!verifyHMAC(content, seqNum, hmac)) {
            System.err.println("Authentication failed for message");
            return;
        }

        // Send authenticated acknowledgment
        try {
            sendAuthenticatedAcknowledgment(seqNum);
        } catch (Exception e) {
            System.err.println("Failed to send acknowledgment: " + e.getMessage());
        }

        // Check for duplicates
        if (isDuplicate(seqNum)) {
            return;
        }

        try {
            Object deserializedObject = SerializationUtils.deserializeObject(content);

            // If the object is a ConsensusMessage
            // TODO this is trash no? -> Duarte
            if (deserializedObject instanceof ConsensusMessage) {
                Logger.LOG("Received ConsensusMessage of type: " +
                        ((ConsensusMessage<?>) deserializedObject).getType());
            }

            // Deliver to application
            if (messageHandler != null) {
                messageHandler.onMessage(destId, content);
            } else {
                Logger.ERROR("No message handler set. Failed to deliver message.");
            }
        } catch (Exception e) {
            Logger.ERROR("Failed to deserialize message content: ", e);
            e.printStackTrace();

            // Still deliver the raw bytes if deserialization fails
            if (messageHandler != null) {
                messageHandler.onMessage(destId, content);
            } else {
                Logger.ERROR("No message handler set. Failed to deliver message.");
            }
        }
    }

    private boolean isDuplicate(long seqNum) {
        return receivedMessages.putIfAbsent(seqNum, Boolean.TRUE) != null;
    }

    private void sendAuthenticatedAcknowledgment(long seqNum) throws Exception {
        try {
            SecretKey secretKey = getSecretKey();

            byte[] hmac = generateHMAC(new byte[0], seqNum, secretKey);

            AckMessage ackMessage = new AckMessage(seqNum, hmac);

            sendUdpPacket(ackMessage.serialize());
        } catch (Exception e) {
            Logger.ERROR("Failed to sign and send ACK", e);
            throw e;
        }
    }

    private void sendUdpPacket(byte[] data) {
        try {
            String[] parts = GeneralUtils.serversId2Addr.get(destId).split(":");
            InetAddress address = InetAddress.getByName(parts[0]);
            int port = Integer.parseInt(parts[1]);

            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (Exception e) {
            Logger.ERROR("Failed to send UDP packet", e);
        }
    }

    private void startRetransmissionScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            // loop through all pending messages and retransmit if necessary
            pendingMessages.forEach((seqNum, message) -> {
                if (message.getCounter() >= message.getCooldown()) {
                    Logger.LOG("Retransmitting message with seqNum: " + seqNum +
                            "\nWaited cooldown: " + message.getCooldown() * 0.5 + "s");
                    try {
                        sendUdpPacket(message.serialize());
                    } catch (Exception e) {
                        System.err.println("Failed to retransmit message: " + e.getMessage());
                    }
                    message.setCounter(1);
                    message.doubleCooldown(); // Exponential backoff
                } else {
                    message.incrementCounter();
                }
            });
        }, Config.RETRANSMISSION_TIME, Config.RETRANSMISSION_TIME, TimeUnit.MILLISECONDS);
    }

    private boolean verifyHMAC(byte[] content, long seqNum, byte[] hmac) {
        try {
            SecretKey secretKey = getSecretKey();

            String data = Integer.toString(destId) + Integer.toString(nodeId) +
                    Arrays.toString(content) + Long.toString(seqNum);

            return CryptoUtils.verifyHMAC(data, secretKey, hmac);
        } catch (Exception e) {
            System.err.println("Failed to verify HMAC: " + e.getMessage());
            return false;
        }
    }

    private byte[] generateHMAC(byte[] content, long seqNum, SecretKey secretKey) {
        try {
            String data = Integer.toString(nodeId) + Integer.toString(destId) +
                    Arrays.toString(content) + Long.toString(seqNum);
            return CryptoUtils.generateHMAC(data, secretKey);
        } catch (Exception e) {
            System.err.println("Failed to generate HMAC: " + e.getMessage());
            return null;
        }
    }

    private SecretKey getOrGenerateSecretKey() {
        Logger.LOG("Getting or generating secret key");
        SecretKey secretKey = getSecretKey();
        if (secretKey == null) {
            Logger.LOG("Generating new secret key");
            secretKey = generateAndShareSecretKey();
            this.secretKey = secretKey;
        }
        return secretKey;
    }

    private SecretKey getSecretKey() {
        return secretKey;
    }

    private SecretKey generateAndShareSecretKey() {
        try {
            SecretKey secretKey = CryptoUtils.generateSymmetricKey();
            byte[] encryptedKey = CryptoUtils.encryptSymmetricKey(secretKey, destPublicKey);

            long seqNum = nextSeqNum++;

            KeyMessage keyMessage = new KeyMessage(encryptedKey, seqNum);
            pendingMessages.put(seqNum, keyMessage);

            Logger.LOG("Sending key: " + secretKey);
            sendUdpPacket(keyMessage.serialize());

            return secretKey;
        } catch (Exception e) {
            System.err.println("Failed to generate and share secret key: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        scheduler.shutdown();
    }
}
