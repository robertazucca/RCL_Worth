package server;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ServerMain {

    public static void main(String[] args)  throws RemoteException  {


        WorthServer server;
        try {
            server = new WorthServer();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException ex) {
            ex.getStackTrace();
        }

    }
}

