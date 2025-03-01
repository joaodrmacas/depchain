package pt.tecnico.ulisboa.network;

import java.net.*;

public class UdpFairLossLink implements FairLossLink {
    private DatagramSocket socket;
    private MessageHandler handler;
    static private int BUFFER_SIZE_DEFAULT = 1024;

    public UdpFairLossLink(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
        startListening();
    }

    @Override
    public void send(String destination, int port, byte[] message) {
        try {
            InetAddress address = InetAddress.getByName(destination);
            DatagramPacket packet = new DatagramPacket(message, message.length, address, port);
            // TODO: delete this print of the message being sent and its content
            System.out.println("Sending message: ");
            Message msg = Message.deserialize(message);
            msg.printMessage();
            System.out.println();
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
                        handler.onMessage(packet.getAddress().getHostAddress(), packet.getData());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

