package pt.tecnico.ulisboa.network;

public interface MessageHandler {
    void onMessage(int sourceId, byte[] message);
}

