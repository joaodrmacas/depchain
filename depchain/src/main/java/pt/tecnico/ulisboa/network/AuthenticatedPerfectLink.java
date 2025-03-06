package pt.tecnico.ulisboa.network;

public interface AuthenticatedPerfectLink {
    void send(String destId,  byte[] message);

    void setMessageHandler(MessageHandler handler);
}