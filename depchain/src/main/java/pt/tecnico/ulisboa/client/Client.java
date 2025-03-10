package pt.tecnico.ulisboa.client;

import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import pt.tecnico.ulisboa.network.APLImpl;
import pt.tecnico.ulisboa.protocol.*;

public class Client {
    private List<APLImpl> servers = new ArrayList<APLImpl>();
    private Map<Integer, PublicKey> publicKeys = new HashMap<Integer, PublicKey>();
    private ClientMessageHandler<String> messageHandler = new ClientMessageHandler<String>();
    private int clientId;
    private int count = 1;
    private String keysDirectory;

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
        setup();
    }

    private void start() {
        Logger.LOG("Starting client " + clientId);

        // Create a cli loop for receiving input and waiting for messages.
        while (true) {
            System.out.println("Enter a message to send to the server: ");
            String message = System.console().readLine();

            if (message == null || message.isEmpty()) {
                continue;
            }

            try {
            messageHandler.reset();

            Logger.LOG("Sending message: " + message);
            sendMessage(message);

            Logger.LOG("Waiting for response...");
            BlockchainResponse<String> response = messageHandler.waitForResponse();

            Logger.LOG("Received response: " + response);
            } catch (Exception e) {
                Logger.LOG("Failed to send message: " + e.getMessage());
            }

        }
    }

    private void sendMessage(String message) {
        Integer num_request = count*Config.MAX_NUM_CLIENTS + clientId;
        BlockchainRequest<String> msg = new BlockchainRequest<String>(num_request, message);
        for (APLImpl server : servers) {
            server.send(msg);
        }
        count++;
    }

    private void setup() {
        try {
            readAllPublicKeys();
            // Create a link to each server in the configuration
            for (int serverId = 0; serverId < Config.NUM_MEMBERS; serverId++) {
                APLImpl server = new APLImpl(clientId, serverId, publicKeys.get(serverId));
                servers.add(server);
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
            publicKeys.put(keyId, publicKey);
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