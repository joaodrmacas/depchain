package pt.tecnico.ulisboa.network;

//TODO: are we going to use this shi?
public interface APL {
    void send(byte[] message);
    void setMessageHandler(MessageHandler handler);
}
