package pt.tecnico.ulisboa.network;

public interface APL {
    void send(byte[] message);
    void setMessageHandler(MessageHandler handler);
}
