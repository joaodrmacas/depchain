package pt.tecnico.ulisboa.network;

import java.io.Serializable;

public interface AuthenticatedPerfectLink {
    void send(int destId, byte[] message);
    void send(int destId, Serializable message);

    void setMessageHandler(MessageHandler handler);
}