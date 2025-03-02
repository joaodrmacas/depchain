package pt.tecnico.ulisboa.network.layers;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.network.message.*;
import java.util.concurrent.*;
import java.security.*;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.net.SocketException;

public class AuthenticatedPerfectLinkImpl implements AuthenticatedPerfectLink {
    private final StubbornLink stubbornLink;
    private final String myId;
    private final Map<String, PublicKey> publicKeys;
    private final PrivateKey privateKey;

    private final Set<String> deliveredMessages = ConcurrentHashMap.newKeySet();

    public AuthenticatedPerfectLinkImpl(String ip, int port,
            PrivateKey privateKey,
            Map<String, PublicKey> publicKeys) throws SocketException {
        this.stubbornLink = new StubbornLinkImpl(ip, port);
        this.myId = ip + ":" + port;
        this.privateKey = privateKey;
        this.publicKeys = publicKeys;
    }

    private byte[] authenticate(String senderId, String receiverId, byte[] message) {
        try {
            // Use the predefined private key for signing
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);

            ByteArrayOutputStream dataToSign = new ByteArrayOutputStream();
            dataToSign.write(senderId.getBytes());
            dataToSign.write(receiverId.getBytes());
            dataToSign.write(message);

            // Generate signature
            signature.update(dataToSign.toByteArray());
            return signature.sign();
        } catch (Exception e) {
            System.err.println("Authentication error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean verifyAuth(String senderId, String receiverId, byte[] message, byte[] mac) {
        try {
            // Get sender's public key from the predefined map
            PublicKey senderKey = publicKeys.get(senderId);
            if (senderKey == null) {
                System.err.println("Public key not found for: " + senderId);
                return false;
            }

            // Create signature verifier with predefined key
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(senderKey);

            // Combine elements for verification
            ByteArrayOutputStream dataToVerify = new ByteArrayOutputStream();
            dataToVerify.write(senderId.getBytes());
            dataToVerify.write(receiverId.getBytes());
            dataToVerify.write(message);

            // Verify signature
            signature.update(dataToVerify.toByteArray());
            return signature.verify(mac);
        } catch (Exception e) {
            System.err.println("Verification error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void send(String destIP, int destPort, byte[] message) {
        String receiverId = destIP + ":" + destPort;

        byte[] mac = authenticate(myId, receiverId, message);

        AuthenticatedMessage authMsg = new AuthenticatedMessage(message, mac, myId);

        stubbornLink.send(destIP, destPort, authMsg.serialize());
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        stubbornLink.setMessageHandler((source, serializedMessage) -> {
            try {
                // Deserialize the message
                Message msg = Message.deserialize(serializedMessage);
                if (msg == null || !(msg instanceof AuthenticatedMessage)) {
                    System.err.println("Received invalid message type");
                    return;
                }

                AuthenticatedMessage authMsg = (AuthenticatedMessage) msg;

                // Extract data
                byte[] content = authMsg.getContent();
                byte[] mac = authMsg.getMac();
                String senderId = authMsg.getSenderId();

                String messageId = senderId + source + Arrays.hashCode(content);
                // TODO acho que isto ta mal
                // Duplicate message, ignore
                if (deliveredMessages.contains(messageId)) {
                    return;
                }

                // Verify authenticity
                if (!verifyAuth(senderId, myId, content, mac)) {
                    System.err.println("Message authentication failed from: " + senderId);
                    return;
                }

                deliveredMessages.add(messageId);

                handler.onMessage(source, content);

            } catch (Exception e) {
                System.err.println("Error handling message: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}