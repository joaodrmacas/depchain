package pt.tecnico.ulisboa.network.layers;

import pt.tecnico.ulisboa.network.*;
import pt.tecnico.ulisboa.network.message.*;


import java.net.*;

public class FairLossLinkImpl implements FairLossLink {
    private DatagramSocket socket;
    private MessageHandler handler;
    static private int BUFFER_SIZE_DEFAULT = 1024;

    public FairLossLinkImpl(String IP, int port) throws SocketException {
        // TODO: ignoring ip cause using localhost
        this.socket = new DatagramSocket(port);
        startListening();
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    public String getIP() {
        // TODO: ignoring ip cause using localhost
        return socket.getLocalAddress().getHostAddress();
    }

    @Override
    public void send(String destination, int port, byte[] message) {
        try {
            InetAddress address = InetAddress.getByName(destination);
            DatagramPacket packet = new DatagramPacket(message, message.length, address, port);

            Message msg = Message.deserialize(message);
            msg.printMessage();
            System.out.println("Sending message to " + destination + ":" + port);
            System.out.println(new String(packet.getData()));

            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.handler = handler;
    }

    private void startListening() {
        System.out.println("Starting server on port:" + socket.getLocalPort());
        new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE_DEFAULT];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    if (handler != null) {
                        String sender = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                        byte[] data = packet.getData();
                        handler.onMessage(sender, data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

