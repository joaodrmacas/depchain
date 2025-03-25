package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.security.PrivateKey;

import pt.tecnico.ulisboa.utils.types.Logger;


public class ServerAplManager extends AplManager {
    public ServerAplManager(String address, Integer port, PrivateKey privateKey) throws SocketException, IOException {
        super(address, port, privateKey);
    }

    @Override
    protected void handleUnknownSender(DatagramPacket packet) {
        //drop it 
        Logger.LOG("Dropping packet.");
        return;
    }
}