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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.SecretKey;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.message.AckMessage;
import pt.tecnico.ulisboa.network.message.DataMessage;
import pt.tecnico.ulisboa.network.message.KeyMessage;
import pt.tecnico.ulisboa.network.message.Message;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.GeneralUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public class AuthenticatedPerfectLinkImpl implements AuthenticatedPerfectLink {
    private final DatagramSocket socket;
    private MessageHandler messageHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Map of pending messages, indexed by the destination ID and sequence number
    private final ConcurrentMap<String, Message> pendingMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Set<Long>> receivedMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Long> nextSeqNum = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> lastReceivedSeqNum = new ConcurrentHashMap<>();

    private static final int BUFFER_SIZE = 4096;

    private final PrivateKey privateKey;
    private final ConcurrentMap<Integer, PublicKey> publicKeys = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, SecretKey> secretKeys = new ConcurrentHashMap<>();
    private final int nodeId;

    private List<Integer> waitingFor; // List of nodes from which we are waiting for a message
    private final Lock messageReceivedLock = new ReentrantLock();
    private final Condition messageReceived = messageReceivedLock.newCondition();

    public AuthenticatedPerfectLinkImpl(int nodeId, PrivateKey privateKey, Map<Integer, PublicKey> publicKeys)
            throws SocketException, GeneralSecurityException, IOException {

        String address = GeneralUtils.id2addr.get(nodeId);
        String destination = address.split(":")[0];
        int port = Integer.parseInt(address.split(":")[1]);

        // print ip and port
        Logger.LOG("IP: " + destination + " Port: " + port);

        this.socket = new DatagramSocket(port, InetAddress.getByName(destination));
        this.nodeId = nodeId;

        this.privateKey = privateKey;
        this.publicKeys.putAll(publicKeys);

        startListening();

        startRetransmissionScheduler();
        Logger.LOG("AuthenticatedPerfectLink started on port: " + port + " with node ID: " + nodeId);
    }

    public void receivedFrom(int senderId) {
        messageReceivedLock.lock();
        try {
            waitingFor.remove(senderId);
            if (waitingFor.isEmpty()) {
                messageReceived.signal();
            }
        } finally {
            messageReceivedLock.unlock();
        }
    }

    private void waitForReplies() {
        // ATTENTION: This method should only be called inside a synchronized block
        try {
            while (!waitingFor.isEmpty()) { // TODO check if this is correct -> Duarte
                messageReceived.await();
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for messages: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void send(int destId, Serializable message) {
        // TODO: este send ta mal. Tas on it ne massas? -> Duarte
        SecretKey secretKey = getOrGenerateSecretKey(destId);

        byte[] messageBytes;
        try {
            messageBytes = SerializationUtils.toByteArray(secretKey);
        } catch (IOException e) {
            System.err.println("Failed to serialize message: " + e.getMessage());
            return;
        }

        long seqNum = nextSeqNum.getOrDefault(destId, 1L);
        nextSeqNum.put(destId, seqNum + 1);

        try {
            byte[] hmac = generateHMAC(destId, messageBytes, seqNum, secretKey);

            DataMessage dataMsg = new DataMessage(messageBytes, seqNum, hmac);

            // Store the message in the pending list
            String messageId = destId + ":" + seqNum;
            pendingMessages.put(messageId, dataMsg);

            sendUdpPacket(destId, dataMsg.serialize());
        } catch (Exception e) {
            System.err.println("Failed to sign and send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // This is just a helper function to send multiple messages at once -> Duarte
    public void send(Map<Integer, Serializable> messages) {
        for (Map.Entry<Integer, Serializable> entry : messages.entrySet()) {
            send(entry.getKey(), entry.getValue());
        }
    }

    public void sendAndWait(int destId, Serializable message) {
        messageReceivedLock.lock();
        try {
            waitingFor.add(destId);
            send(destId, message);
            waitForReplies();
        } finally {
            messageReceivedLock.unlock();
        }
    }

    // This is just a helper function to send multiple messages at once -> Duarte
    public void sendAndWait(Map<Integer, Serializable> messages) {
        messageReceivedLock.lock();
        try {
            for (Map.Entry<Integer, Serializable> entry : messages.entrySet()) {
                waitingFor.add(entry.getKey());
            }
            send(messages);
            waitForReplies();
        } finally {
            messageReceivedLock.unlock();
        }
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    private void startListening() {
        new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Process received packet
                    byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                    String senderaddr = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                    int senderId = GeneralUtils.addr2id.get(senderaddr);

                    // receivedFrom(senderId); // TODO Isto nao pode ser aqui, tem de ser no handle
                    // data se nao da merda com os acks -> Duarte

                    // print the sender
                    Logger.LOG("Received message from: " + senderId);

                    processReceivedPacket(senderId, data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void processReceivedPacket(int senderId, byte[] data) {
        // print in function
        Logger.LOG("Processing message from: " + senderId);
        Message message = Message.deserialize(data);
        if (message == null)
            return;

        Logger.LOG("Seq num: " + message.getSeqNum());

        if (message.getSeqNum() != lastReceivedSeqNum.getOrDefault(senderId, 1L)) {
            Logger.LOG("Received out-of-order message from: " + senderId);
            return;
        }

        lastReceivedSeqNum.put(senderId, message.getSeqNum() + 1);

        switch (message.getType()) {
            case Message.DATA_MESSAGE_TYPE:
                DataMessage dataMessage = (DataMessage) message;
                handleData(dataMessage, senderId);
                break;
            case Message.ACK_MESSAGE_TYPE:
                AckMessage ackMessage = (AckMessage) message;
                handleACK(ackMessage, senderId);
                break;
            case Message.KEY_MESSAGE_TYPE:
                KeyMessage keyMessage = (KeyMessage) message;
                handleKey(keyMessage, senderId);
                break;
            default:
                System.err.println("Unknown message type: " + message.getType());
        }
    }

    private void handleKey(KeyMessage keyMessage, int senderId) {
        // print in function
        Logger.LOG("Received key message from: " + senderId);
        SecretKey secretKey = null;
        try {
            secretKey = CryptoUtils.decryptSymmetricKey(keyMessage.getContent(), privateKey);
        } catch (Exception e) {
            System.err.println("Failed to decrypt symmetric key: " + e.getMessage());
            return;
        }

        secretKeys.put(senderId, secretKey);

        // print the key
        Logger.LOG("Received key: " + secretKey);
        // Send authenticated acknowledgment
        try {
            sendAuthenticatedAcknowledgment(senderId, keyMessage.getSeqNum());
        } catch (Exception e) {
            System.err.println("Failed to send acknowledgment: " + e.getMessage());
        }

        // Check for duplicates
        if (isDuplicate(senderId, keyMessage.getSeqNum())) {
            return;
        }
        Logger.LOG("map: " + secretKeys);
    }

    private void handleACK(AckMessage ackMessage, int senderId) {
        byte[] content = ackMessage.getContent();
        long seqNum = ackMessage.getSeqNum();
        byte[] hmac = ackMessage.getMac();

        if (!verifyHMAC(senderId, content, seqNum, hmac)) {
            System.err.println("Authentication failed for ACK from " + senderId);
            return;
        }

        // Remove the message from the pending list
        String messageId = senderId + ":" + seqNum;

        // print when receiving ack foe unsent message
        if (!pendingMessages.containsKey(messageId)) {
            Logger.LOG("Received ACK for unsent message: " + messageId);
        }

        pendingMessages.remove(messageId);
    }

    private void handleData(DataMessage dataMessage, int senderId) {
        byte[] content = dataMessage.getContent();
        long seqNum = dataMessage.getSeqNum();
        byte[] hmac = dataMessage.getMac();

        if (!verifyHMAC(senderId, content, seqNum, hmac)) {
            System.err.println("Authentication failed for message from " + senderId);
            return;
        }

        // Send authenticated acknowledgment
        try {
            sendAuthenticatedAcknowledgment(senderId, seqNum);
        } catch (Exception e) {
            System.err.println("Failed to send acknowledgment: " + e.getMessage());
        }

        // Check for duplicates
        if (isDuplicate(senderId, seqNum)) {
            return;
        }

        // Remove the sender from the waiting list
        // TODO: How are we sure that the message we are receiving is actually an answer
        // to the message we sent since this can be any message from the guy we are
        // waiting for? Maybe we need to create a new message type for this or sm shi ->
        // Duarte
        receivedFrom(senderId);

        // Deliver to application if not a duplicate and signature is valid
        if (messageHandler != null) {
            messageHandler.onMessage(senderId, content);
        }
    }

    private boolean isDuplicate(int senderId, long seqNum) {
        Set<Long> received = receivedMessages.computeIfAbsent(senderId, k -> ConcurrentHashMap.newKeySet());
        return !received.add(seqNum);
    }

    private void sendAuthenticatedAcknowledgment(int destId, long seqNum) throws Exception {
        try {

            SecretKey secretKey = getSecretKey(destId);

            byte[] hmac = generateHMAC(destId, new byte[0], seqNum, secretKey);

            // Create authenticated ACK message
            AckMessage ackMessage = new AckMessage(seqNum, hmac);

            // Send it
            sendUdpPacket(destId, ackMessage.serialize());
        } catch (Exception e) {
            System.err.println("Failed to sign and send ACK: " + e.getMessage());
            throw e;
        }
    }

    private void sendUdpPacket(int destId, byte[] data) {
        try {
            String[] parts = GeneralUtils.id2addr.get(destId).split(":");
            InetAddress address = InetAddress.getByName(parts[0]); // IP address of the destination
            int port = Integer.parseInt(parts[1]);

            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRetransmissionScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            // loop through all pending messages and retransmit if necessary
            pendingMessages.forEach((messageId, message) -> {
                if (message.getCounter() >= message.getCooldown()) {
                    int nodeId = Integer.parseInt(messageId.split(":")[0]);

                    Logger.LOG("Retransmitting message: " + messageId + "\nWaited cooldown: "
                            + message.getCooldown() * 0.5 + "s");
                    try {
                        sendUdpPacket(nodeId, message.serialize());
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

    private boolean verifyHMAC(int senderId, byte[] content, long seqNum, byte[] hmac) {
        try {
            SecretKey secretKey = getSecretKey(senderId);

            String data = Integer.toString(senderId) + Integer.toString(nodeId) + Arrays.toString(content)
                    + Long.toString(seqNum);

            return CryptoUtils.verifyHMAC(data, secretKey, hmac);

        } catch (Exception e) {
            System.err.println("Failed to verify HMAC: " + e.getMessage());
            return false;
        }
    }

    private byte[] generateHMAC(int destId, byte[] content, long seqNum, SecretKey secretKey) {
        try {
            String data = Integer.toString(nodeId) + Integer.toString(destId) + Arrays.toString(content)
                    + Long.toString(seqNum);
            return CryptoUtils.generateHMAC(data, secretKey);
        } catch (Exception e) {
            System.err.println("Failed to generate HMAC: " + e.getMessage());
            return null;
        }
    }

    private SecretKey getOrGenerateSecretKey(int destId) {
        // print
        Logger.LOG("Getting or generating secret key for: " + destId);
        SecretKey secretKey = getSecretKey(destId);
        if (secretKey == null) {
            Logger.LOG("I am here");
            secretKey = generateAndShareSecretKey(destId);
            secretKeys.put(destId, secretKey);
        }
        return secretKey;
    }

    private SecretKey getSecretKey(int destId) {
        return secretKeys.get(destId);
    }

    private SecretKey generateAndShareSecretKey(int destId) {

        try {

            PublicKey publicKey = publicKeys.get(destId);
            SecretKey secretKey = CryptoUtils.generateSymmetricKey();
            byte[] encryptedKey = CryptoUtils.encryptSymmetricKey(secretKey, publicKey);

            long seqNum = nextSeqNum.getOrDefault(destId, 1L);
            nextSeqNum.put(destId, seqNum + 1);

            KeyMessage keyMessage = new KeyMessage(encryptedKey, seqNum);
            // print sending key
            Logger.LOG("Sending key: " + secretKey);
            sendUdpPacket(destId, keyMessage.serialize());

            return secretKey;

        } catch (Exception e) {
            System.err.println("Failed to generate and share secret key: " + e.getMessage());
            return null;
        }
    }
}