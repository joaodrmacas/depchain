package pt.tecnico.ulisboa;

import pt.tecnico.ulisboa.network.*;
public class App 
{
    public static void main( String[] args )
    {
        // StubbornLink sv1;
        // StubbornLink sv2;
        // try {
        //     sv1 = new StubbornLinkImpl("127.0.0.1", 8080);

        //     sv2 = new StubbornLinkImpl("12", 8081);
        //     sv1.setMessageHandler((sender, data) -> {
        //         System.out.println("sv1 received: " + new String(data));
        //         }
        //     );

        //     sv2.setMessageHandler((sender, data) -> {
        //         System.out.println("sv2 received: " + new String(data));
        //     });

        //     sv1.send("127.0.0.1", 8081, "Boaaa taaardddeee from sv1".getBytes());
        //     // sv2.send("127.0.0.1", 8080, "Boomm ddiiiaaa from sv2".getBytes());
            
        // } catch (Exception e) {
        //     System.out.println("Error creating server");
        //     e.printStackTrace();
        // }
        //make a case where the message is not delivered
        // there is only one guy sending to an unexinting server
        StubbornLink sv3;
        try {
            sv3 = new StubbornLinkImpl("127.0.0.1", 8082);
            sv3.setMessageHandler((sender, data) -> {
                System.out.println("sv3 received: " + new String(data));
            });
            sv3.send("127.0.0.1", 8083, "Boaaa nooooiittteeee from sv3".getBytes());
            // wait for ome time and then create new server to receive
            Thread.sleep(5000);
            StubbornLink sv4 = new StubbornLinkImpl("127.0.0.1", 8083);
            sv4.setMessageHandler((sender, data) -> {
                System.out.println("sv4 received: " + new String(data));
            });
            System.out.println("sv4 created");
        } catch (Exception e) {
            System.out.println("Error creating server");
            e.printStackTrace();
        }
    }
}

