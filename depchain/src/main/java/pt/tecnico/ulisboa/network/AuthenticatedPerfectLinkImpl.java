package pt.tecnico.ulisboa.network;

import pt.tecnico.ulisboa.network.message.*;
import java.util.concurrent.*;
import java.util.*;
import java.net.*;
import java.security.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import pt.tecnico.ulisboa.Config;


public class AuthenticatedPerfectLinkImpl implements AuthenticatedPerfectLink {
    private final DatagramSocket socket;
    private MessageHandler messageHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Map of pending messages, indexed by the destination ID and sequence number
    private final ConcurrentMap<String, AuthenticatedMessage> pendingMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Long>> receivedMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> nextSeqNum = new ConcurrentHashMap<>();

    private static final int BUFFER_SIZE = 4096;

    private final PrivateKey privateKey;
    private final ConcurrentMap<String, PublicKey> publicKeys = new ConcurrentHashMap<>();
    private final String nodeId;

    public AuthenticatedPerfectLinkImpl(String destination, int port, String nodeId, PrivateKey privateKey,
            Map<String, PublicKey> publicKeys) throws SocketException, GeneralSecurityException, IOException {
        this.socket = new DatagramSocket(port, InetAddress.getByName(destination));
        this.nodeId = nodeId;

        this.privateKey = privateKey;
        this.publicKeys.putAll(publicKeys);

        startListening();
        startRetransmissionScheduler();
        System.out.println("AuthenticatedPerfectLink started on port: " + port + " with node ID: " + nodeId);
    }

    public void send(String destId, byte[] message) {
        // Get sequence number for this message
        long seqNum = nextSeqNum.getOrDefault(destId, 1L);
        nextSeqNum.put(destId, seqNum + 1);

        try {
            byte[] hmac = createSignature(nodeId, destId, message, seqNum);

            DataMessage dataMsg = new DataMessage(message, seqNum, hmac);

            // Store the message in the pending list
            String messageId = destId + ":" + seqNum;
            pendingMessages.put(messageId, dataMsg);

            sendUdpPacket(destId, dataMsg.serialize());
        } catch (Exception e) {
            System.err.println("Failed to sign and send message: " + e.getMessage());
            e.printStackTrace();
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
                    String sender = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                    // print the sender
                    System.out.println("Received message from: " + sender);

                    processReceivedPacket(sender, data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void processReceivedPacket(String sender, byte[] data) {
        Message message = Message.deserialize(data);
        if (message == null)
            return;

        switch (message.getType()) {
            case Message.DATA_MESSAGE_TYPE:
                DataMessage dataMessage = (DataMessage) message;
                handleData(dataMessage, sender);
                break;
            case Message.ACK_MESSAGE_TYPE:
                AckMessage ackMessage = (AckMessage) message;
                handleACK(ackMessage, sender);
                break;
            default:
                System.err.println("Unknown message type: " + message.getType());
        }
    }

    private void handleACK(AckMessage ackMessage, String senderId) {
        byte[] content = ackMessage.getContent();
        long seqNum = ackMessage.getSeqNum();
        byte[] mac = ackMessage.getMac();

        // Verify the signature
        if (!verifySignature(senderId, nodeId, content, seqNum, mac)) {
            System.err.println("Authentication failed for ACK from " + senderId);
            return;
        }

        // Remove the message from the pending list
        String messageId = senderId + ":" + seqNum;

        // print when receiving ack foe unsent message
        if (!pendingMessages.containsKey(messageId)) {
            System.out.println("Received ACK for unsent message: " + messageId);
        }

        pendingMessages.remove(messageId);
    }

    private void handleData(DataMessage dataMessage, String senderId) {
        byte[] content = dataMessage.getContent();
        long seqNum = dataMessage.getSeqNum();
        byte[] hmac = dataMessage.getMac();

        // Verify the signature (APL3: Authenticity)
        if (!verifySignature(senderId, nodeId, content, seqNum, hmac)) {
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

        // Deliver to application if not a duplicate and signature is valid
        if (messageHandler != null) {
            messageHandler.onMessage(senderId, content);
        }
    }

    private boolean isDuplicate(String sender, long seqNum) {
        Set<Long> received = receivedMessages.computeIfAbsent(sender, k -> ConcurrentHashMap.newKeySet());
        return !received.add(seqNum);
    }

    private void sendAuthenticatedAcknowledgment(String destID, long seqNum) throws Exception {
        try {
            // Sign the ACK with our private key
            byte[] hmac = createSignature(nodeId, destID, seqNum);

            // Create authenticated ACK message
            AckMessage ackMessage = new AckMessage(seqNum, hmac);

            // Send it
            sendUdpPacket(destID, ackMessage.serialize());
        } catch (Exception e) {
            System.err.println("Failed to sign and send ACK: " + e.getMessage());
            throw e;
        }
    }

    private void sendUdpPacket(String destId, byte[] data) {
        try {
            String[] parts = destId.split(":");
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
                    String destinationId = messageId.split(":")[0] + ":" + messageId.split(":")[1];
                    System.out.println("Retransmitting message: " + messageId + "\nWaited cooldown: "
                            + message.getCooldown() * 0.5 + "s");
                    try {
                        sendUdpPacket(destinationId, message.serialize());
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

    private byte[] createSignature(String senderId, String receiverId, long seqNum)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return createSignature(senderId, receiverId, new byte[0], seqNum);
    }

    /**
     * Creates a signature for the message using the format: SenderId + ReceiverId +
     * Message + SeqNum
     */
    private byte[] createSignature(String senderId, String receiverId, byte[] message, long seqNum)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);

            // Create the byte array with the requested format: SenderId + ReceivedId +
            // Message + SeqNum
            ByteBuffer buffer = ByteBuffer.allocate(
                    senderId.getBytes().length +
                            receiverId.getBytes().length +
                            message.length +
                            8 // 8 bytes for long seqNum
            );

            buffer.put(senderId.getBytes());
            buffer.put(receiverId.getBytes());
            buffer.put(message);
            buffer.putLong(seqNum);

            signature.update(buffer.array());
            return signature.sign();
        } catch (Exception e) {
            System.err.println("Failed to create signature: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Verifies a signature for the message using the format: SenderId + ReceivedId
     * + Message + SeqNum
     */
    private boolean verifySignature(String senderId, String receiverId, byte[] message, long seqNum,
            byte[] signatureBytes) {
        try {
            PublicKey publicKey = publicKeys.get(senderId);
            if (publicKey == null) {
                System.err.println("No public key found for sender: " + senderId);
                return false;
            }

            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);

            // Create the byte array with the same format used for signing
            ByteBuffer buffer = ByteBuffer
                    .allocate(senderId.getBytes().length +
                            receiverId.getBytes().length +
                            message.length + 8);

            buffer.put(senderId.getBytes());
            buffer.put(receiverId.getBytes());
            buffer.put(message);
            buffer.putLong(seqNum);

            verifier.update(buffer.array());
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            System.err.println("Signature verification failed: " + e.getMessage());
            return false;
        }
    }
}