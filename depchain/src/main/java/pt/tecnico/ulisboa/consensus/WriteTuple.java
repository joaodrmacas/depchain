package pt.tecnico.ulisboa.consensus;

import java.security.Signature;
import java.util.Map;

import pt.tecnico.ulisboa.crypto.CryptoUtils;


public class WriteTuple<T> {
    private T value;
    private int ts;
    private byte[] signature;

    public WriteTuple(T value, int ts) {
        this.value = value;
        this.ts = ts;
        this.signature = signData(value.toString() + String.valueOf(ts));
    }
}
