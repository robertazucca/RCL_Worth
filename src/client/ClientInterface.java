package client;

import server.Project;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface ClientInterface extends Remote {

    // metodo che notifica gli stati aggiornati degli utenti dopo che uno di loro ha effettuato il login o il logout.
    void notifyEvent(Map<String, String> usrs) throws RemoteException;

    //metodo che notifica ai client la mappa progetti (di cui sono membri)-indirizzi aggiornata dopo aver effetuato il login
    void notifySockets(Map<Project, String> sock) throws RemoteException;

    //metodo che notifica a un client l'indirizzo per unirsi alla chat quando è stato aggiunto al team di un nuovo progetto
    //oppure dopo l'avvenuta creazione di un nuovo progetto
    void notifyProj(Project p, String add) throws RemoteException, IOException;

    //metodo che notifica il client quando un progetto è stato cancellato, per aggiornare le strutture dati
    //e interrompere il thread chat
    void notifyDelete(String p) throws RemoteException;
}
