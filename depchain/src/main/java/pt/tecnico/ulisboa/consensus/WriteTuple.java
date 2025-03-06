package pt.tecnico.ulisboa.consensus;

import java.io.*;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

import pt.tecnico.ulisboa.crypto.CryptoUtils;

public class WriteTuple<T> implements Serializable {
    private static final long serialVersionUID = 1L; // Ensures serialization compatibility

    private T value;
    private int ts;
    private byte[] signature;

    public WriteTuple(T value, int ts, PrivateKey privKey) {
        this.value = value;
        this.ts = ts;
        this.signature = CryptoUtils.signData(value.toString() + ts, privKey);
    }

    public T getValue() {
        return value;
    }

    public int getTimestamp() {
        return ts;
    }

    public byte[] getSignature() {
        return signature;
    }
}
