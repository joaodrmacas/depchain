package pt.tecnico.ulisboa.consensus;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.tecnico.ulisboa.utils.CryptoUtils;

public class ConsensusState<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private WriteTuple<T> mostRecentQuorumWritten;
    private Map<String, WriteTuple<T>> writeSet = new HashMap<>();

    private byte[] signature;

    public WriteTuple<T> getMostRecentQuorumWritten() {
        return mostRecentQuorumWritten;
    }

    public void setMostRecentQuorumWritten(WriteTuple<T> mostRecentQuorumWritten) {
        this.mostRecentQuorumWritten = mostRecentQuorumWritten;
    }

    public Map<String, WriteTuple<T>> getWriteSet() {
        return writeSet;
    }

    public void addToWriteSet(WriteTuple<T> writeTuple) {
        String key = writeTuple.getValue().toString();

        if (writeSet.containsKey(key)) {
            WriteTuple<T> existing = writeSet.get(key);
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
        String data;

        data = this.mostRecentQuorumWritten.toString();

        List<String> keys = new ArrayList<>(this.writeSet.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            data += this.writeSet.get(key).toString();
        }

        return data;
    }
}
