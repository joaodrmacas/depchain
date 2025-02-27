package pt.tecnico.ulisboa.network;

public interface MessageHandler {
    void onMessage(String source, byte[] message);
}

// massas confia é muito melhor assim
