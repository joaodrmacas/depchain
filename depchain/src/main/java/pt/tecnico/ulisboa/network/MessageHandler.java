package pt.tecnico.ulisboa.network;

public interface MessageHandler {
    public void onMessage(int senderId, byte[] message);
}

