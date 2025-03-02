package pt.tecnico.ulisboa.network.layers;

import pt.tecnico.ulisboa.network.MessageHandler;

public interface FairLossLink {
    void send(String destination, int port, byte[] message);
    void setMessageHandler(MessageHandler handler);
}
