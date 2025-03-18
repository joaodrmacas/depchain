package pt.tecnico.ulisboa.consensus;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.RequiresEquals;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public class ConsensusState<T extends RequiresEquals> implements Serializable {
    private static final long serialVersionUID = 1L;

    private WriteTuple<T> mostRecentQuorumWritten;
    private Map<T, WriteTuple<T>> writeSet = new HashMap<>();

    private String signature = null;

    public ConsensusState(WriteTuple<T> mostRecentQuorumWritten) {
        this.mostRecentQuorumWritten = mostRecentQuorumWritten;
    }

    public WriteTuple<T> getMostRecentQuorumWritten() {
        return mostRecentQuorumWritten;
    }

    public void setMostRecentQuorumWritten(WriteTuple<T> mostRecentQuorumWritten) {
        this.mostRecentQuorumWritten = mostRecentQuorumWritten;
    }

    public Map<T, WriteTuple<T>> getWriteSet() {
        return writeSet;
    }

    public void addToWriteSet(WriteTuple<T> writeTuple) {
        T key = writeTuple.getValue();
        WriteTuple<T> existing = writeSet.get(key);

        if (existing != null) {
            if (existing.getTimestamp() < writeTuple.getTimestamp()) {
                writeSet.put(key, writeTuple);
            }
        } else {
            writeSet.put(key, writeTuple);
        }
    }

    public boolean verifySignature(PublicKey pubKey) {
        if (this.signature != null) {
            return CryptoUtils.verifySignature(this.dataToSign(), this.signature, pubKey);
        }
        return false;
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
            return false;
        }

        for (WriteTuple<T> writeTuple : writeSet.values()) {
            if (!writeTuple.isValid()) {
                return false;
            }
        }

        if (!verifySignature(pubKey)) {
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
