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

import javax.crypto.SecretKey;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.message.AckMessage;
import pt.tecnico.ulisboa.network.message.DataMessage;
import pt.tecnico.ulisboa.network.message.KeyMessage;
import pt.tecnico.ulisboa.network.message.Message;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.Logger;

public class AplManager {
    private MessageHandler messageHandler;
    private final DatagramSocket socket;
    private final ConcurrentHashMap<Long, Boolean> receivedMessages = new ConcurrentHashMap<>();
    private final Map<Integer, APLImpl> aplInstances = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastReceivedSeqNums = new ConcurrentHashMap<>();
    private final PrivateKey privateKey;
    private boolean isRunning = true;

    public AplManager(String address, Integer port, MessageHandler msgHandler, PrivateKey privateKey) throws SocketException, IOException {
        this.privateKey = privateKey;

        Logger.LOG("Creating shared socket - IP: " + address + " Port: " + port);
        this.socket = new DatagramSocket(port, InetAddress.getByName(address));
        this.messageHandler = msgHandler;
        startMessageDispatcher();
    }

    public AplManager(String address, MessageHandler msgHandler, Integer port) throws SocketException, IOException {
        this(address, port, msgHandler, null);
    }

    //TODO: precisamos mm de destID? acho que sim por causa do carro né
    public APLImpl createAPL(Integer destId, String destAddress, Integer destPort, PublicKey destPublicKey) throws Exception {
        if (aplInstances.containsKey(destId)) {
            Logger.LOG("APL instance already exists for destination ID: " + destId + ". Returning existing instance.");
            return aplInstances.get(destId);
        }

        APLImpl apl = new APLImpl(destAddress, destPort, destPublicKey, socket);
        aplInstances.put(destId, apl);
        lastReceivedSeqNums.put(destId, 1L);

        return apl;
    }

    public APLImpl getAPL(int destId) {
        return aplInstances.get(destId);
    }

    public void removeAPL(int destId) {
        aplInstances.remove(destId);
    }

    public void send(int destId, byte[] message) {
        APLImpl apl = aplInstances.get(destId);
        if (apl != null) {
            apl.send(message);
        } else {
            Logger.LOG("No APL instance found for destination ID: " + destId);
        }
    }

    public void send(int destId, Serializable message) {
        APLImpl apl = aplInstances.get(destId);
        if (apl != null) {
            apl.send(message);
        } else {
            Logger.LOG("No APL instance found for destination ID: " + destId);
        }
    }

    // Start the message dispatcher thread
    private void startMessageDispatcher() {
        new Thread(() -> {
            byte[] buffer = new byte[Config.BUFFER_SIZE];

            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Determine the sender

                    handlePacket(packet);
                    
                    //TODO: ???
                    // if (senderAddr== null) {
                    //     Logger.LOG("Received packet from unknown sender: " + senderAddr);
                    //     continue;
                    // }

                    // // Get the APL for this sender and dispatch the packet
                    // APLImpl apl = aplInstances.get(senderId);
                    // if (apl != null) {
                    //     handlePacket(packet);
                    // } else {
                    //     Logger.LOG("No APL instance found for sender ID: " + senderId);
                    // }

                } catch (Exception e) {
                    if (isRunning) {
                        Logger.ERROR("Error receiving packet", e);
                    }
                }
            }
        }).start();
    }

    public boolean handlePacket(DatagramPacket packet) {
        try {
            byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
            String senderaddr = packet.getAddress().getHostAddress() + ":" + packet.getPort();
        
            //TODO: somehow translate senderaddr to senderId. SenderAddrs é forgeable
            int senderId = -1;

            // Only process messages from our designated destination
            Logger.LOG("Received message from destination: " + senderaddr);
            processReceivedPacket(senderId ,data);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void processReceivedPacket(int senderId, byte[] data) {
        Logger.LOG("Processing message from destination");
        Message message = Message.deserialize(data);
        if (message == null)
            return;

        Long lastReceivedSeqNum = lastReceivedSeqNums.get(senderId);
        if ( lastReceivedSeqNum != null && message.getSeqNum() != lastReceivedSeqNum ) {
            Logger.LOG("Received out-of-order message");
            return;
        }

        lastReceivedSeqNums.put(senderId, lastReceivedSeqNum + 1);

        switch (message.getType()) {
            case Message.DATA_MESSAGE_TYPE:
                DataMessage dataMessage = (DataMessage) message;
                handleData(senderId, dataMessage);
                break;
            case Message.ACK_MESSAGE_TYPE:
                AckMessage ackMessage = (AckMessage) message;
                handleACK(senderId, ackMessage);
                break;
            case Message.KEY_MESSAGE_TYPE:
                KeyMessage keyMessage = (KeyMessage) message;
                handleKey(senderId, keyMessage);
                break;
            default:
                System.err.println("Unknown message type: " + message.getType());
        }
    }

    private void handleKey(int senderId, KeyMessage keyMessage) {
        Logger.LOG("Received key message");
        SecretKey secretKey = null;
        try {
            secretKey = CryptoUtils.decryptSymmetricKey(keyMessage.getContent(), privateKey);
        } catch (Exception e) {
            System.err.println("Failed to decrypt symmetric key: " + e.getMessage());
            return;
        }

        APLImpl apl = aplInstances.get(senderId);

        //TODO: Podemos acreditar nisto? Se um atacante der spoof do meu ip, ele fica-me com o link e so precisa de mandar um Keymessage - massas
        //nao deviamos so aceitar se nao existisse ou até mesmo mandar um pong da chave para o client e esperar que ele confirme que está correta a que recebeu e so 
        apl.setSecretKey(secretKey);

        Logger.LOG("Received key: " + secretKey);

        // Send authenticated acknowledgment
        try {
            apl.sendAuthenticatedAcknowledgment(keyMessage.getSeqNum());
        } catch (Exception e) {
            System.err.println("Failed to send acknowledgment: " + e.getMessage());
        }

        // Check for duplicates
        if (isDuplicate(keyMessage.getSeqNum())) {
            return;
        }
    }

    private void handleACK(int senderId, AckMessage ackMessage) {
        byte[] content = ackMessage.getContent();
        long seqNum = ackMessage.getSeqNum();
        byte[] hmac = ackMessage.getMac();

        APLImpl apl = aplInstances.get(senderId);

        if (!verifyHMAC(content, seqNum, hmac, apl.getSecretKey())) {
            System.err.println("Authentication failed for ACK");
            return;
        }

        ConcurrentHashMap<Long,Message> pendingMsgs = apl.getPendingMessages();

        // Remove the message from the pending list
        if (!pendingMsgs.containsKey(seqNum)) {
            Logger.LOG("Received ACK for unsent message: " + seqNum);
        }

        pendingMsgs.remove(seqNum);

    }

    private void handleData(int senderId, DataMessage dataMessage) {
        byte[] content = dataMessage.getContent();
        long seqNum = dataMessage.getSeqNum();
        byte[] hmac = dataMessage.getMac();
        
        APLImpl apl = aplInstances.get(senderId);


        if (!verifyHMAC(content, seqNum, hmac, apl.getSecretKey())) {
            System.err.println("Authentication failed for message");
            return;
        }


        // Send authenticated acknowledgment
        try {
            apl.sendAuthenticatedAcknowledgment(seqNum);
        } catch (Exception e) {
            System.err.println("Failed to send acknowledgment: " + e.getMessage());
        }

        // Check for duplicates
        if (isDuplicate(seqNum)) {
            return;
        }

        try {

            // Deliver to application
            if (messageHandler != null) {
                messageHandler.onMessage(content);
            } else {
                Logger.ERROR("No message handler set. Failed to deliver message.");
            }
        } catch (Exception e) {
            Logger.ERROR("Failed to deserialize message content: ", e);
            e.printStackTrace();

            // Still deliver the raw bytes if deserialization fails
            if (messageHandler != null) {
                messageHandler.onMessage(content);
            } else {
                Logger.ERROR("No message handler set. Failed to deliver message.");
            }
        }
    }

    private boolean isDuplicate(long seqNum) {
        return receivedMessages.putIfAbsent(seqNum, Boolean.TRUE) != null;
    }

    private boolean verifyHMAC(byte[] content, long seqNum, byte[] hmac, SecretKey secretKey) {
        try {
            //TODO: isto é chill ser so o content e seqNum né? fica unico a mm - Massas
            String data = Arrays.toString(content) + Long.toString(seqNum);

            return CryptoUtils.verifyHMAC(data, secretKey, hmac);
        } catch (Exception e) {
            System.err.println("Failed to verify HMAC: " + e.getMessage());
            return false;
        }
    }

    // Clean shutdown
    public void close() {
        isRunning = false;

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

    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

}