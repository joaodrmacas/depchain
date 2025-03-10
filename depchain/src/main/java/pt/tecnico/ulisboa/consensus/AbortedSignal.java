package pt.tecnico.ulisboa.consensus;

public class AbortedSignal extends Exception {
    public AbortedSignal() {
        super("Aborted signal");
    }

    public AbortedSignal(String message) {
        super("Aborted signal: " + message);
    }
}
