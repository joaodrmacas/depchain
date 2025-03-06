package pt.tecnico.ulisboa.network;

public interface AuthenticatedPerfectLink {
    void send(int destId,  byte[] message);

    void setMessageHandler(MessageHandler handler);
}