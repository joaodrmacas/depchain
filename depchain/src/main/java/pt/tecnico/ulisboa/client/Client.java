package pt.tecnico.ulisboa.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.APLImpl;
import pt.tecnico.ulisboa.protocol.AppendReq;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.protocol.KeyRegisterReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.Logger;

public class Client {
    private KeyPair keyPair = CryptoUtils.generateKeyPair(Config.CLIENT_KEYPAIR_SIZE);
    private Map<Integer, APLImpl> serversLinks = new HashMap<>();
    private Map<Integer, PublicKey> serversPublicKeys = new HashMap<Integer, PublicKey>();
    private int clientId;
    private long count = 0;
    private String keysDirectory;
    
    private CountDownLatch responseLatch;

    private ConcurrentHashMap<BlockchainMessage, Integer> currentRequestResponses = new ConcurrentHashMap<>();
    private AtomicReference<BlockchainMessage> acceptedResponse = new AtomicReference<>();
    
    private ClientMessageHandler messageHandler;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: Client <clientId> <keysDirectory>");
            System.exit(1);
        }

        int clientId = Integer.parseInt(args[0]);
        String keysDirectory = args[1];

        Client client = new Client(clientId, keysDirectory);
        client.start();
    }

    public Client(int clientId, String keysDirectory) {
        this.clientId = clientId;
        this.keysDirectory = keysDirectory;
        responseLatch = new CountDownLatch(1);
        this.messageHandler = new ClientMessageHandler(count, currentRequestResponses, responseLatch, acceptedResponse);

        setup();
    }

    private void start() {
        Logger.LOG("Starting client " + clientId);

        while (true) {
            System.out.println("Enter a message to send to the server: ");
            String message = System.console().readLine();

            if (message == null || message.isEmpty()) {
                continue;
            }

            try {
                Logger.LOG("Sending message: " + message);
                sendAppendRequest(message);
                
                // Wait for response
                Logger.LOG("Waiting for server responses...");
                if (!waitForResponse()) {
                    Logger.LOG("Timed out waiting for enough responses");
                } else {
                    BlockchainMessage response = acceptedResponse.get();
                    if (response != null) {
                        Logger.LOG("Accepted response: " + response);
                    }
                }
                
            } catch (Exception e) {
                Logger.LOG("Failed to send message: " + e.getMessage());
            }
        }
    }

    private void sendAppendRequest(String message) {
        if (count == 0) { // First message. Send public key to servers
            sendPublicKeyToServers();
        }

        responseLatch = new CountDownLatch(1);
        currentRequestResponses.clear();
        acceptedResponse.set(null);
        
        messageHandler.updateForNewRequest(count, responseLatch);
        
        String signature = signMessage(message);
        AppendReq<String> msg = new AppendReq<String>(clientId, message, count, signature);
        for (APLImpl server : serversLinks.values()) {
            server.send(msg);
        }
        count++;
    }
    
    public boolean waitForResponse() {
        try {
            return responseLatch.await(Config.CLIENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.LOG("Interrupted while waiting for response");
            return false;
        }
    }

    private void sendPublicKeyToServers() {
        // Create new latch for key registration
        responseLatch = new CountDownLatch(1);
        currentRequestResponses.clear();
        acceptedResponse.set(null);
        
        // Update message handler with the current sequence number and new latch
        messageHandler.updateForNewRequest(count, responseLatch);
        
        PublicKey publicKey = keyPair.getPublic();
        for (APLImpl server : serversLinks.values()) {
            byte[] publicKeyBytes = CryptoUtils.publicKeyToBytes(publicKey);
            server.send(new KeyRegisterReq(publicKeyBytes, count));
        }
        
        // Wait for response to key registration
        try {
            if (!responseLatch.await(Config.CLIENT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                Logger.LOG("Timed out waiting for key registration responses");
            } else {
                Logger.LOG("Key registration successful");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.LOG("Interrupted while waiting for key registration response");
        }
        
        count++;
    }

    private String signMessage(String message) {
        String dataToSign = clientId + message + count;
        return CryptoUtils.signData(dataToSign, keyPair.getPrivate());
    }

    private void setup() {
        try {
            readAllPublicKeys();
            for (int serverId = 0; serverId < Config.NUM_MEMBERS; serverId++) {
                APLImpl server = new APLImpl(clientId, serverId, serversPublicKeys.get(serverId));
                serversLinks.put(serverId, server);
                server.setMessageHandler(messageHandler);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to read public keys", e);
        }
    }

    public void setKeysDirectory(String directory) {
        this.keysDirectory = directory;
    }

    private void readAllPublicKeys() throws Exception {
        File keysDir = new File(keysDirectory);
        if (!keysDir.exists() || !keysDir.isDirectory()) {
            throw new RuntimeException("Keys directory not found: " + keysDir.getAbsolutePath());
        }

        Logger.LOG("Reading public keys from: " + keysDir.getAbsolutePath());

        File[] keyFiles = keysDir.listFiles((dir, name) -> name.startsWith("pub") && name.endsWith(".key"));
        if (keyFiles == null || keyFiles.length == 0) {
            throw new RuntimeException("No public keys found in " + keysDir.getAbsolutePath());
        }

        for (int i = 0; i < keyFiles.length; i++) {
            String keyPath = keyFiles[i].getPath();
            String keyName = keyFiles[i].getName();
            int keyId = Integer.parseInt(keyName.substring(3, 5));
            Logger.LOG("Reading public key from: " + keyPath);

            // Read the PEM format key
            String pemKey = readPemFile(keyFiles[i]);

            // Extract the base64 encoded key data (remove PEM headers and newlines)
            String base64Key = pemKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            // Decode the base64 key data
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);

            // Create the public key
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);
            serversPublicKeys.put(keyId, publicKey);
        }
    }

    private String readPemFile(File file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
