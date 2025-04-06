package pt.tecnico.ulisboa.consensus;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.types.Consensable;
import pt.tecnico.ulisboa.utils.types.Logger;

public class ConsensusState implements Serializable {
    private static final long serialVersionUID = 1L;

    private WriteTuple mostRecentQuorumWritten;
    private Map<Consensable, WriteTuple> writeSet = new HashMap<>();

    private String signature = null;

    public ConsensusState(WriteTuple mostRecentQuorumWritten) {
        this.mostRecentQuorumWritten = mostRecentQuorumWritten;
    }

    public WriteTuple getMostRecentQuorumWritten() {
        return mostRecentQuorumWritten;
    }

    public void setMostRecentQuorumWritten(WriteTuple mostRecentQuorumWritten) {
        this.mostRecentQuorumWritten = mostRecentQuorumWritten;
    }

    public Map<Consensable, WriteTuple> getWriteSet() {
        return writeSet;
    }

    public void addToWriteSet(WriteTuple writeTuple) {
        Consensable key = writeTuple.getValue();
        WriteTuple existing = writeSet.get(key);

        if (existing != null) {
            if (existing.getTimestamp() < writeTuple.getTimestamp()) {
                writeSet.put(key, writeTuple);
            }
        } else {
            writeSet.put(key, writeTuple);
        }
    }

    public boolean verifySignature(PublicKey pubKey) {
        return this.signature != null && CryptoUtils.verifySignature(this.dataToSign(), this.signature, pubKey);
    }

    public void sign(PrivateKey privKey) {
        this.signature = CryptoUtils.signData(this.dataToSign(), privKey);
    }

    private String dataToSign() {
        byte[] dataBytes = null;
        try {
            dataBytes = SerializationUtils.serializeObject(mostRecentQuorumWritten);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CryptoUtils.bytesToBase64(dataBytes);
    }

    public boolean isValid(PublicKey pubKey) {
        if (mostRecentQuorumWritten == null | writeSet == null 
            | !mostRecentQuorumWritten.isValid()) {
            Logger.ERROR("Invalid MRQW");
            
            return false;
        }

        for (WriteTuple writeTuple : writeSet.values()) {
            if (!writeTuple.isValid()) {
                Logger.ERROR("Invalid write tuple");
                return false;
            }
        }

        if (!verifySignature(pubKey)) {
            Logger.ERROR("Invalid signature");
            return false;   
        }

        return true;
    }

    @Override
    public String toString() {
        return toStringShort();

        // return "ConsensusState{" +
        //         "mostRecentQuorumWritten=" + mostRecentQuorumWritten +
        //         ", writeSet=" + writeSet +
        //         ", signature='" + signature + '\'' +
        //         '}';
    }

    public String toStringShort() {
        return "{" +
                "MRQW=" + mostRecentQuorumWritten +
                ", writeSet=" + writeSet +
                '}';
    }
}
