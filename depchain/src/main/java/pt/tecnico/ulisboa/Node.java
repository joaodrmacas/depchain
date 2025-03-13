package pt.tecnico.ulisboa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import pt.tecnico.ulisboa.consensus.BFTConsensus;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessageHandler;
import pt.tecnico.ulisboa.network.APLImpl;
import pt.tecnico.ulisboa.network.ClientAplManager;
import pt.tecnico.ulisboa.network.ServerAplManager;
import pt.tecnico.ulisboa.protocol.AppendResp;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.utils.GeneralUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.ObservedResource;
import pt.tecnico.ulisboa.utils.RequiresEquals;

public class Node<T extends RequiresEquals> {
    private int nodeId;
    private PrivateKey privateKey;
    private ConcurrentHashMap<Integer, PublicKey> publicKeys;
    // TODO: ao termos dois sockets diferentes, os clientes vao ter que comunicar
    // com 2 cenas diferentes, nao sei se é assim tao fixe. - massas
    // E este registerManager nao faz nada porque no fundo o messageHandler e a APL
    // faz tudo por ele
    private ClientAplManager<T> clientManager;
    private ServerAplManager serversManager; // disgusting name
    private String keysDirectory = Config.DEFAULT_KEYS_DIR;
    private ConcurrentHashMap<Integer, PublicKey> clientPublicKeys;

    private ObservedResource<Queue<T>> transactions = new ObservedResource<>(new ConcurrentLinkedQueue<>());
    private Set<T> transactionsSet = ConcurrentHashMap.newKeySet();

    private ObservedResource<Queue<T>> decidedValues = new ObservedResource<>(new ConcurrentLinkedQueue<>());
    private Set<T> decidedValuesSet = ConcurrentHashMap.newKeySet();

    private Map<Integer, ObservedResource<Queue<ConsensusMessage<T>>>> consensusMessages = new HashMap<>();
    private ArrayList<T> blockchain;

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
            Node<BlockchainMessage> node = new Node<BlockchainMessage>(nodeId);

            if (args.length >= 2) {
                node.setKeysDirectory(args[1]);
            }

            node.setup(address, clientPort, serverPort);
            node.mainLoop();

        } catch (Exception e) {
            Logger.ERROR("Node setup failed", e);
            System.exit(1);
        }
    }

    public Node(int nodeId) {
        this.nodeId = nodeId;
        this.publicKeys = new ConcurrentHashMap<>();
    }

    public APLImpl getLink(int destId) {
        if (destId == nodeId) {
            Logger.ERROR("Cannot create a link to self");
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
                    // consensus thread already changes the decided queue
                    decidedValues.waitForChange(Integer.MAX_VALUE);
                    T value = decidedValues.getResource().poll();
                    handleDecidedValue(value);
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

    private void handleDecidedValue(T value) {
        boolean success = false;
        // TODO: verificar se foi um abort. Se sim, temos de mandar false no success ao
        // cliente.
        if (value != null) {
            success = true;
            // Add to blockchain
            blockchain.add(value);
            Logger.LOG("Decided value: " + value);
        }

        LocalDateTime timestamp = LocalDateTime.now();

        // Send answer to clients
        clientManager.send(value.getSenderId(), new AppendResp(success, timestamp));
    }

    public void setup(String address, int portRegister, int port) {
        Logger.LOG("Setting up node " + nodeId);
        try {
            // Read private key for this node
            readPrivateKey();

            // Read all public keys
            readAllPublicKeys();

            // Initialize the consensus messages queues
            for (int destId : publicKeys.keySet()) {
                if (destId == nodeId) {
                    continue;
                }
                this.consensusMessages.put(destId, new ObservedResource<>(new LinkedList<>()));
            }

            serversManager = new ServerAplManager(address, port, privateKey);

            // Initialize APLs, one for each destination node

            for (int destId : publicKeys.keySet()) {
                if (destId >= Config.NUM_MEMBERS){
                    break;
                }
                if (destId == nodeId) {
                    continue;
                }

                Logger.LOG("Creating APL for destination node " + destId);

                String destAddr = GeneralUtils.id2Addr.get(destId);
                int destPort = GeneralUtils.id2ServerPort.get(destId);

                Logger.LOG("Creating APL for destination node " + destAddr + ":" + destPort);


                ConsensusMessageHandler<T> handler = new ConsensusMessageHandler<>(consensusMessages);
                serversManager.createAPL(destId, destAddr, destPort, publicKeys.get(destId), handler);
                Logger.LOG("APL created for destination node " + destId);
            }

            // Initialize register APL
            clientManager = new ClientAplManager<>(address, portRegister, privateKey, transactions, clientPublicKeys);

            Logger.LOG("Node setup complete");

        } catch (Exception e) {
            System.err.println("Setup failed: " + e.getMessage());
            throw new RuntimeException("Node setup failed", e);
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

    public void pushReceivedTx(T value) {
        if (decidedValuesSet.contains(value)) {
            Logger.LOG("Received a transaction that was already decided: " + value);
            return;
        }

        transactionsSet.add(value);
        transactions.getResource().add(value);
        transactions.notifyChange();
    }

    public T peekReceivedTxOrWait() {
        while (true) {
            T value = transactions.getResource().peek();
            if (value != null) {
                return value;
            }

            try {
                boolean hasTimeouted = !transactions.waitForChange(Config.LINK_TIMEOUT);

                if (hasTimeouted) {
                    return null;
                }
            } catch (Exception e) {
                Logger.ERROR("Exception: ", e);
            }
        }
    }

    public T peekReceivedTx() {
        return transactions.getResource().peek();
    }

    public void removeReceivedTx(T value) {
        if (transactionsSet.contains(value)) {
            transactionsSet.remove(value);
            transactions.getResource().remove(value);
            transactions.notifyChange();
        }
    }

    public void pushDecidedTx(T value) {
        decidedValuesSet.add(value);
        decidedValues.getResource().add(value);
        decidedValues.notifyChange();
    }

    public ConsensusMessage<T> pollConsensusMessageOrWait(int senderId) {
        while (true) {
            ConsensusMessage<T> msg = consensusMessages.get(senderId).getResource().poll();
            if (msg != null) {
                return msg;
            }

            try {
                boolean hasTimeouted = !consensusMessages.get(senderId).waitForChange(Config.LINK_TIMEOUT);

                if (hasTimeouted) {
                    return null;
                }
            } catch (Exception e) {
                Logger.ERROR("Exception: ", e);
            }
        }
    }

    public ConsensusMessage<T> peekConsensusMessageOrWait(int senderId) {
        while (true) {
            ConsensusMessage<T> msg = consensusMessages.get(senderId).getResource().peek();
            if (msg != null) {
                return msg;
            }

            try {
                boolean hasTimeouted = !consensusMessages.get(senderId).waitForChange(Config.LINK_TIMEOUT);

                if (hasTimeouted) {
                    return null;
                }
            } catch (Exception e) {
                Logger.ERROR("Exception: ", e);
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
        serversManager.send(memberId, msg);
    }
}