package pt.tecnico.ulisboa;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.network.UdpFairLossLink;

public class App 
{
    public static void main( String[] args )
    {
        UdpFairLossLink sv;
        try {
            sv = new UdpFairLossLink(8080);
            sv.setMessageHandler(new MessageHandler());

        } catch (Exception e) {
            System.out.println("Error creating server");
            e.printStackTrace();
        }
        
    }
}
