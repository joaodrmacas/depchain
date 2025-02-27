package pt.tecnico.ulisboa.network;

public class MessageHandler {
    
    public MessageHandler(){
        
    }

    public void onMessage(String source, byte[] message) {
        System.out.println("Received message from " + source + " with length " + message.length);
    }
}

