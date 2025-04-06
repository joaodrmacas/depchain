package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.SecretKey;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.message.AckMessage;
import pt.tecnico.ulisboa.network.message.AuthenticatedMessage;
import pt.tecnico.ulisboa.network.message.DataMessage;
import pt.tecnico.ulisboa.network.message.FragmentedMessage;
import pt.tecnico.ulisboa.network.message.KeyMessage;
import pt.tecnico.ulisboa.network.message.Message;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class APLImpl implements APL {
    private MessageHandler messageHandler;
    private final DatagramSocket socket;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final ConcurrentHashMap<Long, Message> pendingMessages = new ConcurrentHashMap<>();

    private AtomicLong nextSeqNum = new AtomicLong(1);
    private AtomicLong lastReceivedSeqNum = new AtomicLong(0);

    private final PrivateKey privateKey;
    private final PublicKey destPublicKey;
    private SecretKey secretKey;

    InetAddress nodeAddress;
    Integer nodePort;

    InetAddress destAddress;
    Integer destPort;

    // Constructor that accepts an existing socket
    public APLImpl(String nodeAddress, Integer nodePort, String destAddress, Integer destPort, PrivateKey privateKey,
            PublicKey destPublicKey, DatagramSocket socket, MessageHandler messageHandler)
            throws GeneralSecurityException, IOException {
        this.nodeAddress = InetAddress.getByName(nodeAddress);
        this.nodePort = nodePort;
        this.destAddress = InetAddress.getByName(destAddress);
        this.destPort = destPort;
        this.socket = socket;
        this.privateKey = privateKey;
        this.destPublicKey = destPublicKey;
        setMessageHandler(messageHandler);

        startRetransmissionScheduler();
    }

    // // For unit tests
    // public APLImpl(int nodeId, int destId, PrivateKey privateKey, PublicKey
    // destPublicKey,
    // MessageHandler messageHandler)
    // throws SocketException, GeneralSecurityException, IOException {
    // this(nodeId, destId, privateKey, destPublicKey);
    // this.messageHandler = messageHandler;
    // }

    // No public key needed. This is used for clients
    public APLImpl(String nodeAddress, Integer nodePort, String destAddress, Integer destPort, PrivateKey privateKey,
            DatagramSocket socket, MessageHandler messageHandler)
            throws GeneralSecurityException, IOException {
        this(nodeAddress, nodePort, destAddress, destPort, privateKey, null, socket, messageHandler);
    }

    public void send(Serializable obj) {
        byte[] messageBytes = null;
        try {
            messageBytes = SerializationUtils.serializeObject(obj);
        } catch (IOException e) {
            Logger.ERROR("Failed to serialize message: " + e.getMessage(), e);
            return;
        }
        send(messageBytes);
    }

    public void send(byte[] message) {
        SecretKey secretKey = getOrGenerateSecretKey();
        long seqNum = nextSeqNum.getAndIncrement();

        byte[] hmac = generateHMAC(message, seqNum, secretKey);
        DataMessage dataMsg = new DataMessage(message, seqNum, hmac);

        Logger.LOG(destPort + ") Sending datamessage of seqnum " + seqNum);

        // Store the message in the pending list
        pendingMessages.put(seqNum, dataMsg);

        message = dataMsg.serialize();

        // Fragment the message
        fragmentAndSend(message, seqNum);
    }

    public void fragmentAndSend(byte[] message, long seqNum) {
        // Fragment the message
        String msgId = nodePort.toString() + seqNum;

        int fragmentCount = (int) Math.ceil((double) message.length / Config.MAX_FRAGMENT_SIZE);
        for (int i = 0; i < fragmentCount; i++) {
            int startIndex = i * Config.MAX_FRAGMENT_SIZE;
            int endIndex = Math.min(startIndex + Config.MAX_FRAGMENT_SIZE, message.length);

            byte[] fragment = Arrays.copyOfRange(message, startIndex, endIndex);

            FragmentedMessage fragmentMsg = new FragmentedMessage(fragment, i, fragmentCount, message.length, msgId);

            try {
                sendUdpPacket(SerializationUtils.serializeObject(fragmentMsg));
            } catch (IOException e) {
                Logger.ERROR("Failed to send fragment: " + e.getMessage(), e);
            }
        }
    }

    // This method is used to test the integrity of the message
    public void sendAndTamper(Serializable obj) {
        byte[] message = null;
        try {
            message = SerializationUtils.serializeObject(obj);
        } catch (IOException e) {
            Logger.ERROR("Failed to serialize message: " + e.getMessage(), e);
            return;
        }
        SecretKey secretKey = getOrGenerateSecretKey();
        long seqNum = nextSeqNum.getAndIncrement();
        // print the seqnum
        Logger.LOG("Port: " + this.destPort + " Next seqnum: " + nextSeqNum.get());

        try {
            byte[] hmac = generateHMAC(message, seqNum, secretKey);

            // Tamper the message
            byte[] tampered = "tampered".getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(message.length + tampered.length);
            buffer.put(message);
            buffer.put(tampered);
            byte[] tamperedMessage = buffer.array();

            DataMessage dataMsg = new DataMessage(tamperedMessage, seqNum, hmac);

            // Store the message in the pending list
            pendingMessages.put(seqNum, dataMsg);

            sendUdpPacket(dataMsg.serialize());
        } catch (Exception e) {
            Logger.ERROR("Failed to sign and send message: " + e.getMessage(), e);
        }
    }

    public void sendWithTimeout(Serializable obj, int timeout) {
        byte[] messageBytes = null;
        try {
            messageBytes = SerializationUtils.serializeObject(obj);
        } catch (IOException e) {
            Logger.ERROR("Failed to serialize message: " + e.getMessage(), e);
            return;
        }
        sendWithTimeout(messageBytes, timeout);
    }

    public void sendWithTimeout(byte[] message, int timeout) {
        SecretKey secretKey = getOrGenerateSecretKey();
        long seqNum = nextSeqNum.getAndIncrement();

        byte[] hmac = generateHMAC(message, seqNum, secretKey);
        DataMessage dataMsg = new DataMessage(message, seqNum, hmac, timeout);

        // Store the message in the pending list
        pendingMessages.put(seqNum, dataMsg);

        message = dataMsg.serialize();

        fragmentAndSend(message, seqNum);
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    public boolean handleMessage(int senderId, byte[] data) {
        try {
            processReceivedPacket(senderId, data);
            return true;
        } catch (Exception e) {
            Logger.ERROR("Failed to handle data: ", e);
            return false;
        }
    }

    protected void processReceivedPacket(int senderId, byte[] data) {
        Message message = Message.deserialize(data);
        if (message == null) {
            return;
        }

        // Logger.LOG(destPort + ") Received message: " + message.toStringExtended());

        switch (message.getType()) {
            case Message.DATA_MESSAGE_TYPE:
                DataMessage dataMessage = (DataMessage) message;
                if (isOutOfOrder(message, senderId)) {
                    Logger.LOG("Message was out of order. Ignoring.");
                    return;
                }
                handleData(senderId, dataMessage);
                break;
            case Message.ACK_MESSAGE_TYPE:
                AckMessage ackMessage = (AckMessage) message;
                handleACK(ackMessage);
                break;
            case Message.KEY_MESSAGE_TYPE:
                KeyMessage keyMessage = (KeyMessage) message;
                if (isOutOfOrder(message, senderId)) {
                    Logger.LOG("Message was out of order. Ignoring.");
                    return;
                }
                handleKey(keyMessage);
                break;
            default:
                Logger.ERROR("Unknown message type: " + message.getType());
        }
    }

    private boolean isOutOfOrder(Message message, int senderId) {
        if (!lastReceivedSeqNum.compareAndSet(message.getSeqNum() - 1, message.getSeqNum())) {
            if (lastReceivedSeqNum.get() >= message.getSeqNum()) {
                Logger.LOG("Received message was already received: Message seqnum: " + message.getSeqNum() + ". From: "
                        + senderId + ". Last received: " + lastReceivedSeqNum.get());
                try {
                    sendAuthenticatedAcknowledgment(message.getSeqNum());
                } catch (Exception e) {
                    Logger.LOG("Failed to send acknowledgment: " + e.getMessage());
                }
            } else {
                Logger.LOG(
                        "Received message was from the future: Message seqnum: " + message.getSeqNum() + ". From: "
                                + senderId
                                + ". Last received: " + lastReceivedSeqNum.get());
            }
            return true;
        }
        return false;
    }

    private void handleKey(KeyMessage keyMessage) {
        Logger.LOG("Received key message");
        SecretKey secretKey = null;
        try {
            secretKey = CryptoUtils.decryptSymmetricKey(keyMessage.getContent(), privateKey);
        } catch (Exception e) {
            Logger.ERROR("Failed to decrypt symmetric key: " + e.getMessage(), e);
            return;
        }

        Logger.LOG("Current key: " + this.secretKey);

        this.secretKey = secretKey;

        Logger.LOG("Received key: " + secretKey);

        // Send authenticated acknowledgment
        try {
            // Logger.LOG(destPort + ")" + "SENDING ACKNOWLEDGMENT FOR KEY");
            sendAuthenticatedAcknowledgment(keyMessage.getSeqNum());
        } catch (Exception e) {
            Logger.LOG("Failed to send acknowledgment: " + e.getMessage());
        }
    }

    private void handleACK(AckMessage ackMessage) {
        long seqNum = ackMessage.getSeqNum();
        byte[] hmac = ackMessage.getMac();

        if (!verifyHMAC(seqNum, hmac)) {
            Logger.LOG("Authentication failed for ACK: " + seqNum);
            return;
        }

        // for (Map.Entry<Long, Message> entry : pendingMessages.entrySet()) {
        // Logger.LOG("Pending message: " + entry.getKey());
        // }

        // Remove the message from the pending list
        if (!pendingMessages.containsKey(seqNum)) {
            // Logger.LOG("Received ACK for unsent message: " + seqNum);
            return;
        }

        // Logger.LOG("Received ACK for message: " + seqNum);

        pendingMessages.remove(seqNum);

        // print messages in pending list
        // Logger.LOG("...\nPending messages after:");
        // for (Long key : pendingMessages.keySet()) {
        // Logger.LOG("Pending message: " + key);
        // }
        // Logger.LOG("...");
    }

    private void handleData(Integer senderId, DataMessage dataMessage) {
        byte[] content = dataMessage.getContent();
        long seqNum = dataMessage.getSeqNum();
        byte[] hmac = dataMessage.getMac();

        if (!verifyHMAC(content, seqNum, hmac)) {
            Logger.LOG("Authentication failed for message");
            return;
        }

        // Send authenticated acknowledgment
        try {
            sendAuthenticatedAcknowledgment(seqNum);
        } catch (Exception e) {
            Logger.ERROR("Failed to send acknowledgment: " + e.getMessage(), e);
        }

        try {

            // Deliver to application
            if (messageHandler != null) {
                messageHandler.onMessage(senderId, content);
            } else {
                Logger.ERROR("No message handler set. Failed to deliver message.", new Exception());
            }
        } catch (Exception e) {
            Logger.ERROR("Failed to deserialize message content: ", e);

            // Still deliver the raw bytes if deserialization fails
            if (messageHandler != null) {
                messageHandler.onMessage(senderId, content);
            } else {
                Logger.ERROR("No message handler set. Failed to deliver message.", new Exception());
            }
        }
    }

    private void sendAuthenticatedAcknowledgment(long dataSeqNum) throws Exception {
        try {
            SecretKey secretKey = getSecretKey();
            long seqNum = dataSeqNum;
            // create the content from the dataSeqNum

            byte[] hmac = generateHMAC(seqNum, secretKey);

            AckMessage ackMessage = new AckMessage(seqNum, hmac);

            // Logger.LOG(destPort + ") " + "Sending ACK of seqNum: " + seqNum);

            byte[] byteMsg = ackMessage.serialize();

            String msgId = "ack_" + nodePort.toString() + seqNum;

            FragmentedMessage msg = new FragmentedMessage(byteMsg, 0, 1, byteMsg.length, msgId);

            sendUdpPacket(SerializationUtils.serializeObject(msg));
        } catch (Exception e) {
            Logger.ERROR("Failed to sign and send ACK", e);
            throw e;
        }
    }

    private void sendUdpPacket(byte[] data) {
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, destAddress, destPort);
            socket.send(packet);
        } catch (Exception e) {
            Logger.ERROR("Failed to send UDP packet", e);
        }
    }

    private void startRetransmissionScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            Set<Long> timedOutSeqNums = new HashSet<>();

            // Check timeouts
            for (Map.Entry<Long, Message> entry : pendingMessages.entrySet()) {
                Long seqNum = entry.getKey();
                Message message = entry.getValue();

                if (message.getCounter() >= message.getTimeout()) {
                    Logger.LOG("Message timed out: " + seqNum);
                    timedOutSeqNums.add(seqNum);
                }
            }

            // handle timeouts
            if (!timedOutSeqNums.isEmpty()) {
                // print the word aqui 20 times in 20 dif lines
                for (int i = 0; i < 20; i++) {
                    Logger.LOG("aqui");
                }
                // print the timed out messages
                Logger.LOG("Timed out messages: " + timedOutSeqNums);
                // print the pending messages
                Logger.LOG("Pending messages: " + pendingMessages.keySet());
                for (Long seqNum : timedOutSeqNums) {
                    pendingMessages.remove(seqNum);
                }

                Logger.LOG("Decreasing every message's seqnum");

                Long lowestTimedOutSeqNum = Collections.min(timedOutSeqNums);
                List<Message> messagesToUpdate = new ArrayList<>();

                for (Message msg : pendingMessages.values()) {
                    if (msg.getSeqNum() > lowestTimedOutSeqNum) {
                        messagesToUpdate.add(msg);
                    }
                }

                messagesToUpdate.sort((m1, m2) -> Long.compare(m2.getSeqNum(), m1.getSeqNum()));

                // Update sequence numbers
                for (Message msg : messagesToUpdate) {
                    pendingMessages.remove(msg.getSeqNum());
                    msg.setSeqNum(msg.getSeqNum() - timedOutSeqNums.size());
                    // if message is a AuthenticatedMessage, update the hmac
                    if (msg instanceof AuthenticatedMessage) {
                        AuthenticatedMessage authenticatedMessage = (AuthenticatedMessage) msg;
                        byte[] hmac = generateHMAC(authenticatedMessage.getContent(), authenticatedMessage.getSeqNum(),
                                getSecretKey());
                        authenticatedMessage.setHmac(hmac);
                    }
                    pendingMessages.put(msg.getSeqNum(), msg);
                }

                nextSeqNum.addAndGet(-timedOutSeqNums.size());
                Logger.LOG("Port: " + this.destPort + " Next seqnum: " + nextSeqNum.get());

                // print the pending messages
                Logger.LOG("Pending messages after: " + pendingMessages.keySet());

                return;
            }

            for (Map.Entry<Long, Message> entry : pendingMessages.entrySet()) {
                Long seqNum = entry.getKey();
                Message message = entry.getValue();

                if (message.getCounter() >= message.getCooldown()) {
                    Logger.LOG(destPort + ") " + "Retransmitting message with seqNum: " + seqNum +
                            "\nWaited cooldown: " + message.getCooldown() + "ms");
                    try {
                        fragmentAndSend(message.serialize(), seqNum);
                    } catch (Exception e) {
                        Logger.ERROR("Failed to retransmit message: " + e.getMessage(), e);
                    }
                    message.setCounter(0);
                    message.doubleCooldown(); // Exponential backoff
                } else {
                    message.incrementCounter();
                }
            }
        }, Config.RETRANSMISSION_TIME, Config.RETRANSMISSION_TIME, TimeUnit.MILLISECONDS);
    }

    private boolean verifyHMAC(long seqNum, byte[] hmac) {
        return verifyHMAC(new byte[0], seqNum, hmac);
    }

    private boolean verifyHMAC(byte[] content, long seqNum, byte[] hmac) {
        try {
            SecretKey secretKey = getSecretKey();

            String data = nodeAddress.toString() + String.valueOf(nodePort) +
                    Arrays.toString(content) + Long.toString(seqNum);

            return CryptoUtils.verifyHMAC(data, secretKey, hmac);
        } catch (Exception e) {
            Logger.ERROR("Failed to verify HMAC: " + e.getMessage(), e);
            return false;
        }
    }

    private byte[] generateHMAC(byte[] content, long seqNum, SecretKey secretKey) {
        try {

            String data = destAddress.toString() + String.valueOf(destPort) +
                    Arrays.toString(content) + Long.toString(seqNum);

            return CryptoUtils.generateHMAC(data, secretKey);
        } catch (Exception e) {
            Logger.ERROR("Failed to generate HMAC: " + e.getMessage(), e);
            return null;
        }
    }

    private byte[] generateHMAC(long seqNum, SecretKey secretKey) {
        return generateHMAC(new byte[0], seqNum, secretKey);
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

            long seqNum = nextSeqNum.getAndIncrement();
            // print the seqnum
            Logger.LOG("Port: " + this.destPort + " Next seqnum: " + nextSeqNum.get());

            KeyMessage keyMessage = new KeyMessage(encryptedKey, seqNum);
            pendingMessages.put(seqNum, keyMessage);

            Logger.LOG("Sending key: " + secretKey);

            FragmentedMessage frag = new FragmentedMessage(keyMessage.serialize(), 0, 1, keyMessage.serialize().length,
                    nodePort.toString() + seqNum);

            sendUdpPacket(SerializationUtils.serializeObject(frag));

            return secretKey;
        } catch (Exception e) {
            Logger.ERROR("Failed to generate and share secret key: " + e.getMessage(), e);
            return null;
        }
    }

    public void close() {
        scheduler.shutdown();
    }
}