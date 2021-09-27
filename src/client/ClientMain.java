package client;

import java.io.IOException;
import java.rmi.NotBoundException;

public class ClientMain {

    public static void main (String[] args) {

            WorthClient client = new WorthClient();
            //Thread t = new Thread(client);
            client.start();

    }
}
