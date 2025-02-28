package pt.tecnico.ulisboa;

import pt.tecnico.ulisboa.network.*;
public class App 
{
    public static void main( String[] args )
    {
        StubbornLink sv1;
        StubbornLink sv2;
        try {
            sv1 = new StubbornLinkImpl(8080);
            sv2 = new StubbornLinkImpl(8081);
            sv1.setMessageHandler((sender, data) -> {
                System.out.println("sv1 received: " + new String(data));
            });

            sv2.setMessageHandler((sender, data) -> {
                System.out.println("sv2 received: " + new String(data));
            });

            sv1.send("127.0.0.1", 8081, "Boaaa taaardddeee from sv1".getBytes());
            sv2.send("127.0.0.1", 8080, "Boomm ddiiiaaa from sv2".getBytes());
            
        } catch (Exception e) {
            System.out.println("Error creating server");
            e.printStackTrace();
        }

    }
}

