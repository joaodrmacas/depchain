package pt.tecnico.ulisboa.consensus;

public class UnsynchronizedNodeException extends Exception {
    public UnsynchronizedNodeException() {
        super("Unsynchronized node");
    }
    
}
