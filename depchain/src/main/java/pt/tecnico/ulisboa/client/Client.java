package pt.tecnico.ulisboa.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jnr.ffi.annotations.In;
import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.ServerAplManager;
import pt.tecnico.ulisboa.protocol.IsBlacklistedReq;
import pt.tecnico.ulisboa.protocol.CheckBalanceReq;
import pt.tecnico.ulisboa.protocol.GetAllowanceReq;
import pt.tecnico.ulisboa.protocol.ApproveReq;
import pt.tecnico.ulisboa.protocol.BlacklistReq;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.RegisterReq;
import pt.tecnico.ulisboa.protocol.TransferFromReq;
import pt.tecnico.ulisboa.protocol.TransferReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.GeneralUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class Client {
    private KeyPair keyPair = CryptoUtils.generateKeyPair(Config.CLIENT_KEYPAIR_SIZE);
    private ServerAplManager aplManager;
    private Map<Integer, PublicKey> serversPublicKeys = new HashMap<Integer, PublicKey>();
    private Integer clientId;
    private Long count = 0L;
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
        // int port = Integer.parseInt(args[1]);
        String keysDirectory = args[1];

        int port = Config.DEFAULT_CLIENT_PORT + clientId;

        Client client = new Client(clientId, "127.0.0.1", port, keysDirectory);
        client.start();
    }

    public Client(int clientId, String addr, int port, String keysDirectory) {
        this.clientId = clientId;
        this.keysDirectory = keysDirectory;
        responseLatch = new CountDownLatch(1);
        this.messageHandler = new ClientMessageHandler(count, currentRequestResponses, responseLatch, acceptedResponse);

        setup(addr, port);
    }

    private void start() {
        Logger.LOG("Starting client " + clientId);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            // Display available commands
            System.out.println("\n===== Blockchain Client Menu =====");
            System.out.println("Available Commands:");
            System.out.println("1. TRANSFER <from> <to> <amount>");
            System.out.println("   Example: TRANSFER alice bob 100.50");
            System.out.println("2. TRANSFER_FROM <spender> <from> <to> <amount>");
            System.out.println("   Example: TRANSFER_FROM charlie alice bob 50.25");
            System.out.println("3. BLACKLIST <address> <true/false>");
            System.out.println("   Example: BLACKLIST bob true");
            System.out.println("4. APPROVE <owner> <spender> <amount>");
            System.out.println("   Example: APPROVE alice charlie 200.00");
            System.out.println("5. EXIT - Terminate the client");
            System.out.println("====================================");
            System.out.print("Enter a command: ");

            String input = scanner.nextLine().trim();

            if (input == null || input.isEmpty()) {
                continue;
            }

            // Check for exit command
            if (input.equalsIgnoreCase("EXIT")) {
                Logger.LOG("Client shutting down");
                break;
            }

            try {
                Logger.LOG("Sending input: " + input);
                sendRequest(input);

                // Wait for response
                Logger.LOG("Waiting for server responses...");
                if (!waitForResponse()) {
                    Logger.LOG("Timed out waiting for enough responses");
                } else {
                    Logger.DEBUG("Got a response");
                    BlockchainMessage response = acceptedResponse.get();
                    if (response != null) {
                        Logger.LOG("Accepted response: " + response);
                    }
                }
            } catch (Exception e) {
                Logger.ERROR("Failed to send message.", e);
            }
        }

        scanner.close();
    }

    private void sendRequest(String input) {
        if (count == 0) { // First message. Send public key to servers
            sendPublicKeyToServers();
        }

        responseLatch = new CountDownLatch(1);
        currentRequestResponses.clear();
        acceptedResponse.set(null);
        messageHandler.updateForNewRequest(count, responseLatch);

        String[] parts = input.trim().split("\\s+");
        ClientReq req = null;
        String signature = null;

        try {
            switch (parts[0].toUpperCase()) {
                case "CHECK_BALANCE":
                    // Format: CHECK_BALANCE
                    if (parts.length != 1) {
                        throw new IllegalArgumentException("Invalid CHECK_BALANCE format");
                    }
                    req = new CheckBalanceReq(clientId, count);
                    break;

                case "IS_BLACKLISTED":
                    // Format: IS_BLACKLISTED <address>
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Invalid IS_BLACKLISTED format");
                    }
                    req = new IsBlacklistedReq(clientId, count, Integer.parseInt(parts[1]));
                    break;

                case "GET_ALLOWANCE":
                    // Format: GET_ALLOWANCE <allower>
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Invalid GET_ALLOWANCE format");
                    }
                    req = new GetAllowanceReq(clientId, count, Integer.parseInt(parts[1]));
                    break;

                case "TRANSFER":
                    // Format: TRANSFER <to> <amount>
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("Invalid TRANSFER format");
                    }
                    req = new TransferReq(
                            clientId,
                            count,
                            Integer.parseInt(parts[1]),
                            new BigInteger(parts[2]));
                    break;

                case "TRANSFER_FROM":
                    // Format: TRANSFER_FROM <from> <to> <amount>
                    if (parts.length != 4) {
                        throw new IllegalArgumentException("Invalid TRANSFER_FROM format");
                    }
                    req = new TransferFromReq(
                            clientId,
                            count,
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            new BigInteger(parts[3]));
                    break;

                case "BLACKLIST":
                    // Format: BLACKLIST <address> <true/false>
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("Invalid BLACKLIST format");
                    }
                    req = new BlacklistReq(
                            clientId,
                            count,
                            Integer.parseInt(parts[1]),
                            Boolean.parseBoolean(parts[2]));
                    break;

                case "APPROVE":
                    // Format: APPROVE <allowee> <amount>
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("Invalid APPROVE format");
                    }
                    req = new ApproveReq(
                            clientId,
                            count,
                            Integer.parseInt(parts[1]),
                            new BigInteger(parts[2]));
                    break;

                default:
                    System.out.println("Invalid command: " + parts[0]);
                    return;
            }

            // Sign the request
            signature = CryptoUtils.signData(req.toString(), keyPair.getPrivate());
            req.setSignature(signature);

            // Send to leader (server with ID 0)
            aplManager.sendWithTimeout(0, req, Config.CLIENT_TIMEOUT_MS);
            count++;

        } catch (Exception e) {
            Logger.LOG("Error processing request: " + e.getMessage());
        }
    }

    public boolean waitForResponse() {
        try {
            return responseLatch.await(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
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

        Logger.LOG("PUBLIC KEY: " + publicKey);

        // // TODO: fix this to send periodically (...). Only send to leader change
        // later plsp ls pls
        // for (int serverId = 0; serverId < Config.NUM_MEMBERS; serverId++) {
        // byte[] publicKeyBytes = CryptoUtils.publicKeyToBytes(publicKey);
        // aplManager.sendWithTimeout(serverId, new RegisterReq(clientId,
        // publicKeyBytes, count),
        // Config.CLIENT_TIMEOUT_MS);
        // }
        aplManager.sendWithTimeout(0, new RegisterReq(
                clientId,
                CryptoUtils.publicKeyToBytes(publicKey),
                count), Config.CLIENT_TIMEOUT_MS);

        count++;
    }

    private void setup(String addr, int port) {
        try {
            readAllPublicKeys();
            aplManager = new ServerAplManager(addr, port, keyPair.getPrivate());
            Logger.LOG("Creating APLs for all servers");
            for (int serverId = 0; serverId < Config.NUM_MEMBERS; serverId++) {
                Logger.LOG("Creating APL for server " + serverId);
                String adr = GeneralUtils.id2Addr.get(serverId);
                int serverPort = GeneralUtils.id2ClientPort.get(serverId);
                Logger.LOG("Server address: " + adr + ":" + serverPort);
                aplManager.createAPL(serverId, adr, serverPort, serversPublicKeys.get(serverId), messageHandler);
            }
            aplManager.startListening();
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