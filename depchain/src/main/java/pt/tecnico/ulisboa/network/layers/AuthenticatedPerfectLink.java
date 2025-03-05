package pt.tecnico.ulisboa.network.layers;

import pt.tecnico.ulisboa.network.MessageHandler;

public interface AuthenticatedPerfectLink {
    void send(String destId,  byte[] message);

    void setMessageHandler(MessageHandler handler);
}