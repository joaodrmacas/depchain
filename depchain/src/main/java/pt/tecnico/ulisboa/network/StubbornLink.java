package pt.tecnico.ulisboa.network;

public interface StubbornLink {
    void send(String destination, int port, byte[] message);
    void setMessageHandler(MessageHandler handler);
}