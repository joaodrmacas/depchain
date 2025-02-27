package pt.tecnico.ulisboa.network;

public interface FairLossLink {
    void send(String destination, int port, byte[] message);
    void setMessageHandler(MessageHandler handler);
}
