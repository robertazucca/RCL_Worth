package server;

import client.ClientInterface;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIServer extends Remote {

        //metodo per registrarsi al server
        String register (String name, String password) throws IOException, RemoteException;
        //metodo per rendersi disponibile alle callback- al login
        void registerForCallback(ClientInterface stub, String user) throws RemoteException;
        //metodo per non rendersi piu disponibili alle callback- al logout
        void unregisterForCallback(String user) throws RemoteException;

}


