package pt.tecnico.ulisboa.network;

public interface MessageHandler {
    void onMessage(byte[] message);
}

