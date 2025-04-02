package pt.tecnico.ulisboa.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hyperledger.besu.datatypes.Address;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.consensus.BFTConsensus;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessageHandler;
import pt.tecnico.ulisboa.network.APLImpl;
import pt.tecnico.ulisboa.network.ClientAplManager;
import pt.tecnico.ulisboa.network.ServerAplManager;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.ClientResp;
import pt.tecnico.ulisboa.utils.GeneralUtils;
import pt.tecnico.ulisboa.utils.types.Logger;
import pt.tecnico.ulisboa.utils.types.ObservedResource;
import pt.tecnico.ulisboa.utils.types.RequiresEquals;

public class Server<T extends RequiresEquals> {
    private int nodeId;
    private PrivateKey privateKey;
    private ConcurrentHashMap<Integer, PublicKey> publicKeys;
    private ClientAplManager<T> clientManager;
    private ServerAplManager serversManager;
    private String keysDirectory = Config.DEFAULT_KEYS_DIR;
    private ConcurrentHashMap<Integer, PublicKey> clientPublicKeys = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Address> userAddresses = new ConcurrentHashMap<>();

    private ObservedResource<Queue<T>> transactions = new ObservedResource<>(new ConcurrentLinkedQueue<>());

    private ObservedResource<Queue<T>> decidedValues = new ObservedResource<>(new ConcurrentLinkedQueue<>());
    private Set<T> decidedValuesSet = ConcurrentHashMap.newKeySet();

    private Map<Integer, ObservedResource<Queue<ConsensusMessage<T>>>> consensusMessages = new HashMap<>();
    private BlockchainManager<T> blockchainManager = new BlockchainManager<>();

    // TODO: should be closed: exec.shutdown();
    private ExecutorService exec = Executors.newFixedThreadPool(Config.NUM_MEMBERS * 10);

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Node <node-id> [keys-directory]");
            System.exit(1);
        }

        int nodeId = Integer.parseInt(args[0]);

        String address = GeneralUtils.id2Addr.get(nodeId);
        int clientPort = GeneralUtils.id2ClientPort.get(nodeId);
        int serverPort = GeneralUtils.id2ServerPort.get(nodeId);

        try {
            Server<ClientReq> node = new Server<>(nodeId);

            if (args.length >= 2) {
                node.setKeysDirectory(args[1]);
            }

            node.setup(address, clientPort, serverPort);
            node.mainLoop();

        } catch (Exception e) {
            Logger.ERROR("Node setup failed", e);
        }
    }

    public Server(int nodeId) {
        this.nodeId = nodeId;
        this.publicKeys = new ConcurrentHashMap<>();
    }

    public APLImpl getLink(int destId) {
        if (destId == nodeId) {
            Logger.ERROR("Cannot create a link to self", new Exception());
        }

        return serversManager.getAPL(destId);
    }

    public Map<Integer, PublicKey> getPublicKeys() {
        return publicKeys;
    }

    public void setKeysDirectory(String directory) {
        this.keysDirectory = directory;
    }

    public void mainLoop() {

        Thread valueHandlerThread = new Thread(() -> {
            try {
                while (true) {
                    T value = decidedValues.getResource().poll();
                    if (value != null) {
                        ClientResp response = blockchainManager.handleDecidedValue(value);

                        Logger.DEBUG("Sending response to client " + value.getSenderId() + ": " + response.toString());
                        clientManager.send(value.getSenderId(), response);
                    }
                    // consensus thread already changes the decided queue
                    decidedValues.waitForChange(-1);
                }
            } catch (Exception e) {
                Logger.ERROR("Value handler thread failed with exception", e);
                // TODO: Handle recovery or shutdown as appropriate (this should not happen tho)
            }
        });

        BFTConsensus<T> consensusLoop = new BFTConsensus<>(this);
        Thread consensusThread = new Thread(() -> {
            try {
                consensusLoop.start();
            } catch (Exception e) {
                Logger.ERROR("Consensus thread failed with exception", e);
                // TODO: Handle recovery or shutdown as appropriate (this should not happen tho)
            }
        });

        consensusThread.setName("BFT-Consensus-Thread");
        valueHandlerThread.setName("Value-Handler-Thread");

        consensusThread.start();
        valueHandlerThread.start();
    }

    public void setup(String address, int portRegister, int port) {
        Logger.LOG("Setting up node " + nodeId);
        try {
            // Read private key for this node
            readPrivateKey();

            // Read all public keys
            readAllPublicKeys();

            // Initialize the consensus messages queues
            for (int i = 0; i < Config.NUM_MEMBERS; i++) {
                if (i == nodeId) {
                    continue;
                }
                this.consensusMessages.put(i, new ObservedResource<>(new LinkedList<>()));
            }

            serversManager = new ServerAplManager(address, port, privateKey);

            // Initialize APLs, one for each destination node

            for (int i = 0; i < Config.NUM_MEMBERS; i++) {
                if (i >= Config.NUM_MEMBERS) {
                    break;
                }
                if (i == nodeId) {
                    continue;
                }

                Logger.LOG("Creating APL for destination node " + i);

                String destAddr = GeneralUtils.id2Addr.get(i);
                int destPort = GeneralUtils.id2ServerPort.get(i);

                Logger.LOG("Creating APL for destination node " + destAddr + ":" + destPort);

                ConsensusMessageHandler<T> handler = new ConsensusMessageHandler<>(consensusMessages);
                serversManager.createAPL(i, destAddr, destPort, publicKeys.get(i), handler);
                Logger.LOG("APL created for destination node " + i);
            }
            serversManager.startListening();

            // Initialize register APL
            clientManager = new ClientAplManager<>(this, address, portRegister, privateKey, clientPublicKeys, userAddresses);
            clientManager.startListening();

            Logger.LOG("Node setup complete");

        } catch (Exception e) {
            Logger.ERROR("Setup failed: " + e.getMessage(), e);
        }
    }

    private void readPrivateKey() throws Exception {
        String privateKeyPath = String.format("%s/priv%02d.key", keysDirectory, nodeId);
        Logger.LOG("Reading private key from: " + privateKeyPath);

        File privateKeyFile = new File(privateKeyPath);
        if (!privateKeyFile.exists()) {
            throw new RuntimeException("Private key file not found: " + privateKeyFile.getAbsolutePath());
        }

        // Read the PEM format key
        String pemKey = readPemFile(privateKeyFile);

        // Extract the base64 encoded key data (remove PEM headers and newlines)
        String base64Key = pemKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        // Decode the base64 key data
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);

        // Create the private key
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        privateKey = keyFactory.generatePrivate(spec);
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

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
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

        if (Logger.IS_DEBUGGING()) {
            Logger.DEBUG("public keys map");
            System.err.println(publicKeys);
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

    public void pushReceivedTx(T value) {
        transactions.getResource().add(value);
        transactions.notifyChange();
    }

    public T peekReceivedTx() {
        while (true) {
            T value = transactions.getResource().peek();
            if (value != null) {
                if (decidedValuesSet.contains(value)) {
                    Logger.DEBUG("Transaction already decided: " + value);
                    
                    synchronized (transactions.getResource()) {
                        T firstValue = transactions.getResource().peek();
                        if (firstValue.equals(value)) {
                            transactions.getResource().poll();
                        }
                    }

                    continue;
                }
                return value;
            }
            return null;
        }
    }

    public T peekReceivedTxOrWait(Integer timeout) throws InterruptedException {
        while (true) {
            T value = transactions.getResource().peek();
            if (value != null) {
                if (decidedValuesSet.contains(value)) {
                    Logger.DEBUG("Transaction already decided: " + value);

                    synchronized (transactions.getResource()) {
                        T firstValue = transactions.getResource().peek();
                        if (firstValue.equals(value)) {
                            transactions.getResource().poll();
                        }
                    }

                    continue;
                }
                return value;
            }

            boolean hasTimedOut = !transactions.waitForChange(timeout);

            if (hasTimedOut) {
                return null;
            }
        }
    }

    public void pushDecidedTx(T value) {
        decidedValuesSet.add(value);
        decidedValues.getResource().add(value);
        decidedValues.notifyChange();
    }

    public ConsensusMessage<T> pollConsensusMessageOrWait(int senderId, int timeout) throws InterruptedException {
        while (true) {
            ConsensusMessage<T> msg = consensusMessages.get(senderId).getResource().poll();
            if (msg != null) {
                return msg;
            }

            boolean hasTimedOut = !consensusMessages.get(senderId).waitForChange(timeout);

            if (hasTimedOut) {
                return null;
            }
        }
    }

    public ConsensusMessage<T> peekConsensusMessageOrWait(int senderId, int timeout) throws InterruptedException {
        while (true) {
            ConsensusMessage<T> msg = consensusMessages.get(senderId).getResource().peek();
            if (msg != null) {
                return msg;
            }

            boolean hasTimedOut = !consensusMessages.get(senderId).waitForChange(timeout);

            if (hasTimedOut) {
                if (Logger.IS_DEBUGGING()) {
                    Logger.DEBUG("consensus messages:");
                    printConsensusMessages();
                }

                return null;
            }
        }
    }

    public void removeFirstConsensusMessage(int senderId) {
        consensusMessages.get(senderId).getResource().poll();
    }

    public int getId() {
        return nodeId;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void sendToMember(int memberId, ConsensusMessage<T> msg) {
        Logger.LOG((memberId + 8080) + ") Sending message: DT{Consensus{" + msg.toString() + "}}");

        serversManager.send(memberId, msg);
    }

    public void printReceivedTxs() {
        String str = "***** BEGIN Received transactions: \n";

        for (T value : transactions.getResource()) {
            str += value.toString() + "\n";
        }

        str += "***** END Received transactions\n";
        System.err.println(str);
    }

    public void printDecidedValues() {
        String str = "***** BEGIN Decided values: \n";

        for (T value : decidedValues.getResource()) {
            str += value.toString() + "\n";
        }

        str += "***** END Decided values\n";
        System.err.println(str);
    }

    public void printConsensusMessages() {
        String str = "***** BEGIN Consensus messages: \n";

        for (int i = 0; i < Config.NUM_MEMBERS; i++) {
            if (i == nodeId)
                continue;

            str += "\nDestination: " + i + "\n";
            for (ConsensusMessage<T> msg : consensusMessages.get(i).getResource()) {
                str += msg.toString() + "\n";
            }
        }

        str += "\n***** END Consensus messages\n";
        System.err.println(str);
    }

    public ExecutorService getExecutor() {
        return exec;
    }
}