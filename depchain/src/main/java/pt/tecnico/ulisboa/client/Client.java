package pt.tecnico.ulisboa.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hyperledger.besu.datatypes.Address;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.ServerAplManager;
import pt.tecnico.ulisboa.protocol.BalanceOfDepCoinReq;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.ClientResp;
import pt.tecnico.ulisboa.protocol.ContractCallReq;
import pt.tecnico.ulisboa.protocol.RegisterReq;
import pt.tecnico.ulisboa.protocol.TransferDepCoinReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.GeneralUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class Client {
    private KeyPair keyPair = CryptoUtils.generateKeyPair(Config.CLIENT_KEYPAIR_SIZE);
    private ServerAplManager aplManager;
    private Map<Integer, PublicKey> serversPublicKeys = new HashMap<Integer, PublicKey>();
    private Integer clientId;
    private Long count = 1L;
    private String keysDirectory;

    private CountDownLatch responseLatch;

    private ConcurrentHashMap<ClientResp, Integer> currentRequestResponses = new ConcurrentHashMap<>();
    private AtomicReference<ClientResp> acceptedResponse = new AtomicReference<>();

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
            System.out.println("Native Blockchain Commands:");
            System.out.println("- TRANSFER_DEPCOIN <to_id> <amount>");
            System.out.println("  Example: TRANSFER_DEPCOIN 2 100");
            System.out.println("  Transfers DepCoin directly between accounts\n");

            System.out.println("Contract Call Format: <CONTRACT_NAME> <FUNCTION_NAME> [ARGS...]");
            System.out.println("\nExample Contract Commands:");
            System.out.println("1. MergedContract balanceOf");
            System.out.println("   - Check your token balance");
            System.out.println("2. MergedContract transfer <to_id> <amount>");
            System.out.println("   - Example: MergedContract transfer 2 100");
            System.out.println("3. MergedContract transferFrom <from_id> <to_id> <amount>");
            System.out.println("   - Example: MergedContract transferFrom 1 2 50");
            System.out.println("4. MergedContract approve <spender_id> <amount>");
            System.out.println("   - Example: MergedContract approve 3 200");
            System.out.println("5. MergedContract allowance <owner_id>");
            System.out.println("   - Example: MergedContract allowance 1");
            System.out.println("6. MergedContract isBlacklisted <account_id>");
            System.out.println("   - Example: MergedContract isBlacklisted 2");
            System.out.println("7. MergedContract addToBlacklist <account_id>");
            System.out.println("   - Example: MergedContract addToBlacklist 4");
            System.out.println("8. MergedContract removeFromBlacklist <account_id>");
            System.out.println("   - Example: MergedContract removeFromBlacklist 4");
            System.out.println("9. MergedContract buy <amount>");
            System.out.println("   - Example: MergedContract buy 500");
            System.out.println("10. EXIT - Terminate the client");
            System.out.println("\nAvailable Contracts:");
            for (String contractName : Config.CONTRACT_NAME_2_ADDR.keySet()) {
                System.out.println(" - " + contractName);
            }
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
                sendRequest(input);
                // Wait for response
                Logger.LOG("Waiting for server responses...");
                if (!waitForResponse()) {
                    Logger.LOG("Timed out waiting for enough responses");
                } else {
                    Logger.DEBUG("GOT A RESPONSE");
                    ClientResp response = acceptedResponse.get();
                    if (response.getSuccess()) {
                        Logger.LOG("Accepted response: " + response.getMessage());
                    } else {
                        Logger.LOG("Rejected response: " + response.getMessage());
                    }
                }
            } catch (Exception e) {
                Logger.LOG("Failed to send message." + e.getMessage());
            }
        }
        scanner.close();
    }

    private void sendRequest(String input) {

        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) {
            System.out.println("Invalid format. Please enter a valid command.");
            return;
        }

        ClientReq req = null;
        String command = parts[0].toUpperCase();

        // Handle special cases for native blockchain operations
        if (command.equals("TRANSFER_DEPCOIN")) {
            // Format: TRANSFER_DEPCOIN <to_id> <amount>
            if (parts.length != 3) {
                throw new IllegalArgumentException(
                        "Invalid TRANSFER_DEPCOIN format. Usage: TRANSFER_DEPCOIN <to_id> <amount>");
            }

            int toId = Integer.parseInt(parts[1]);
            String toAddress = Config.CLIENT_ID_2_ADDR.get(toId);
            if (toAddress == null) {
                throw new IllegalArgumentException("Unknown client ID: " + toId);
            }

            BigInteger amount = new BigInteger(parts[2]);
            req = new TransferDepCoinReq(clientId, count, toAddress, amount);
        }
        else if (command.equals("BALANCE_OF")){
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid BALANCE_OF format. Usage: BALANCE_OF <client_id>");
            }

            int clientId = Integer.parseInt(parts[1]);
            String clientAddress = Config.CLIENT_ID_2_ADDR.get(clientId);
            if (clientAddress == null) {
                throw new IllegalArgumentException("Unknown client ID: " + clientId);
            }

            BigInteger amount = new BigInteger(parts[2]);
            req = new BalanceOfDepCoinReq(clientId, count);
        }
        // Contract call handling
        else {
            // Check if this is a contract call (needs at least contract name and function)
            if (parts.length < 2) {
                throw new IllegalArgumentException(
                        "Invalid format. For contract calls use: <CONTRACT_NAME> <FUNCTION_NAME> [ARGS...]");
            }

            String contractName = parts[0];
            String functionName = parts[1];

            req = buildContractRequest(contractName, functionName,
                    Arrays.copyOfRange(parts, 2, parts.length));
        }

        if (req != null) {
            if (count == 1) { // First message. Send public key to servers
                sendPublicKeyToServers();
            }
            // Initialize response tracking for the new request
            responseLatch = new CountDownLatch(1);
            currentRequestResponses.clear();
            acceptedResponse.set(null);
            messageHandler.updateForNewRequest(count, responseLatch);
            Logger.LOG("Count: " + count);

            // Sign the request
            Logger.LOG("Signing request: " + req);
            String signature = CryptoUtils.signData(req.toString(), keyPair.getPrivate());
            req.setSignature(signature);

            // TODO: Only sending to the leader. Should be good tho(?)
            Logger.LOG("Sending request: " + req);
            aplManager.sendWithTimeout(Config.LEADER_ID, req, Config.CLIENT_TIMEOUT_MS);
            count++;
            return; // Successfully sent a request
        } else {
            throw new IllegalArgumentException("Failed to create a valid request.");
        }
    }

    /**
     * Builds a contract call request based on the contract address, method
     * signature, and arguments.
     * 
     * @param contractAddress The address of the contract
     * @param methodSignature The signature of the method to call
     * @param args            The arguments for the function call
     * @return A ContractCallReq object representing the request
     * @throws IllegalArgumentException If the arguments are invalid
     */
    private ClientReq buildContractRequest(String contractName, String functionName, String[] args)
            throws IllegalArgumentException {
        try {
            // Verify contract exists
            String contractAddress = Config.CONTRACT_NAME_2_ADDR.get(contractName);
            if (contractAddress == null) {
                throw new IllegalArgumentException("Unknown contract: " + contractName);
            }

            // Verify function exists for this contract
            Map<String, String> methodSignatures = Config.CONTRACT_METHOD_SIGNATURES.get(contractName);
            if (methodSignatures == null || !methodSignatures.containsKey(functionName)) {
                throw new IllegalArgumentException(
                        "Unknown function '" + functionName + "' for contract '" + contractName + "'");
            }

            String methodSignature = methodSignatures.get(functionName);
            switch (functionName) {
                // No arguments functions
                case "balanceOf":
                    if (args.length != 0) {
                        throw new IllegalArgumentException("balanceOf takes no arguments");
                    }
                    return new ContractCallReq(clientId, count, contractAddress, methodSignature, Config.CLIENT_ID_2_ADDR.get(clientId));

                // Single address functions
                case "isBlacklisted":
                case "allowance":
                case "addToBlacklist":
                case "removeFromBlacklist":
                    if (args.length != 1) {
                        throw new IllegalArgumentException(methodSignature + " requires one client ID argument");
                    }
                    Address address = parseAddress(args[0]);
                    return new ContractCallReq(clientId, count, contractAddress, methodSignature, address);

                // Address and amount functions
                case "transfer":
                case "approve":
                    if (args.length != 2) {
                        throw new IllegalArgumentException(
                                methodSignature + " requires client ID and amount arguments");
                    }
                    Address to = parseAddress(args[0]);
                    BigInteger amount = new BigInteger(args[1]);
                    return new ContractCallReq(clientId, count, contractAddress, methodSignature, to, amount);

                // Transfer from function (2 addresses + amount)
                case "transferFrom":
                    if (args.length != 3) {
                        throw new IllegalArgumentException(
                                "transferFrom requires from ID, to ID, and amount arguments");
                    }
                    Address from = parseAddress(args[0]);
                    Address to2 = parseAddress(args[1]);
                    BigInteger amount2 = new BigInteger(args[2]);
                    return new ContractCallReq(clientId, count, contractAddress, methodSignature, from, to2, amount2);

                // Buy function (amount)
                case "buy":
                    if (args.length != 1) {
                        throw new IllegalArgumentException("buy requires an amount argument");
                    }
                    BigInteger buyAmount = new BigInteger(args[0]);
                    BigInteger value = buyAmount.multiply(Config.DEPCOIN_PER_IST);
                    return new ContractCallReq(clientId, count, contractAddress, methodSignature, value, buyAmount);

                default:
                    throw new IllegalArgumentException("Unsupported method: " + methodSignature);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in arguments");
        }
    }

    /**
     * Parses a client ID into an Address
     * 
     * @param clientIdStr The client ID as a string
     * @return The corresponding Address
     * @throws IllegalArgumentException If the client ID is invalid
     */
    private Address parseAddress(String clientIdStr) throws IllegalArgumentException {
        try {
            int clientId = Integer.parseInt(clientIdStr);
            Address address = Address.fromHexString(Config.CLIENT_ID_2_ADDR.get(clientId));
            if (address == null) {
                throw new IllegalArgumentException("Unknown client ID: " + clientId);
            }
            return address;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid client ID format: " + clientIdStr);
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
        for (int serverId = 0; serverId < Config.NUM_MEMBERS; serverId++) {
            byte[] publicKeyBytes = CryptoUtils.publicKeyToBytes(publicKey);
            aplManager.sendWithTimeout(serverId, new RegisterReq(clientId,
                    publicKeyBytes, 0), // TODO: isto ta nojento mas funciona
                    Config.CLIENT_TIMEOUT_MS);
        }
        // aplManager.sendWithTimeout(0, new RegisterReq(
        // clientId,
        // CryptoUtils.publicKeyToBytes(publicKey),
        // count), Config.CLIENT_TIMEOUT_MS);

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