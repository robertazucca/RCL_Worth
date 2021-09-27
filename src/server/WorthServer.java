package server;

import client.ClientInterface;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class WorthServer extends RemoteServer implements RMIServer {

    private List<User> utenti; //lista contenente gli utenti di Worth
    private List<Project> progetti; //lista dei progetti di Worth
    public List<String> utentiOnline; //lista contenente i soli utenti che hanno effettuato il login

    public Map<String, ClientInterface> clients; //map contenente l'associazione tra nome e client degli utenti registrati per le Callbacks

    private final ManagerDB files;


    public WorthServer() throws IOException, AlreadyBoundException {

        super();
        this.files = new ManagerDB();

        //al momento dell'inizializzazione vengono memorizzati i dati presenti in memoria
        //recuperati dal ManagerDB
        this.utenti = new CopyOnWriteArrayList<>(files.getUtenti());
        this.progetti = new CopyOnWriteArrayList<>(files.getProjects());

        this.utentiOnline = new CopyOnWriteArrayList<>();

        this.clients = new ConcurrentHashMap<>();


        //allocazione risorse RMI
        RMIServer stub1 = (RMIServer) UnicastRemoteObject.exportObject(this, 30001);
        LocateRegistry.createRegistry(6789);
        Registry registerRegistry = LocateRegistry.getRegistry(6789);
        registerRegistry.bind("WORTH", stub1);
        System.out.println("Server RMI avviato sulla porta: " + 6789);

        //avvio thread per la gestione delle connesioni TCP
        TCPServer TCPServer = new TCPServer(this);
        new Thread(TCPServer).start();

    }

    //funzione ausiliaria che restituisce un progetto a partire dal nome
    private Project getProject(String projectName) {
        for (Project i : progetti)
            // Controllo tra tutti i progetti se esiste un progetto con nome projectName e lo restituisco
            if (i.getProjectName().equals(projectName)) {
                return i;
            }
        // Se non c'è restituisco null
        return null;
    }

    //metodo per registrare un utente al server
    public synchronized String register(String nickname, String password) throws IOException {

        System.out.println("Utente " + nickname + " desidera registrarsi.");

        if (nickname==null || password==null) return "ERRORE: inserire parametri non vuoti";
        if (password.length() < 5) return "ERRORE: la password deve contenere almeno 5 caratteri";

        utenti = files.getUtenti();

        //controllo che l'utente non sia già presente
        for (User u : utenti) {
            if (u.getNickName().equals(nickname))
                return "ERRORE: utente " + nickname + " gia' presente";
        }

        //creo l'utente
        User newUser = new WorthUser(nickname, password);
        //viene aggiornata la struttura dati del server
        utenti.add(newUser);
        //vengono aggiornati i file degli utenti in memoria
        files.aggiornaUtenti(newUser);

        return "OK";
    }

    //metodo per aggiungersi alla lista
    public synchronized void registerForCallback(ClientInterface stub, String user) throws RemoteException {
        if (!clients.containsValue(stub)) clients.put(user, stub);
    }

    //metodo per cancellarsi dalla lista
    public synchronized void unregisterForCallback(String user) throws RemoteException {
        clients.remove(user);
    }


    //metodo per inviare la mappa aggiornata degli utenti-stati
    public synchronized void doCallBacks() throws RemoteException, IOException {

        Map<String, String> out = new HashMap<>();

        //viene creata la mappa da inviare:
        //per ogni utente Worth se è contenuto nella lista di utenti online lo stato associato sarà online
        //per i restanti lo stato associato sarà offline
        for (User i : utenti) {
            if (utentiOnline.contains(i.getNickName())) {
                out.put(i.getNickName(), "online");
            } else {
                out.put(i.getNickName(), "offline");
            }
        }

        //notifica ad ogni client registrato la mappa con gli stati aggiornati
        for (ClientInterface c : clients.values()) {
            try {
                c.notifyEvent(out);
            } catch (IOException ex) {
            }
        }
    }


    //metodo per inviare all'utente user la mappa aggiornata dei suoi progetti
    public synchronized void doCallBacks2(String user) throws IOException {

        Map<Project, String> out = new HashMap<>();

        for (Project p : progetti) {
                if(p.getMembers().contains(user)) {
                    out.put(p, ((WorthProject)p).getChatAddress());
                }
        }

        ClientInterface c = clients.get(user);

        if(c!=null) {
            c.notifySockets(out);
        }
    }

    //metodo per notificare l'utente user:
    //quando un nuovo progetto viene creato viene mandato il progetto e l'indirizzo per la chat al creatore
    //quando un nuovo membro viene aggiunto viene mandato il progetto e l'indirizzo per la chat al nuovo membro

    public synchronized void doCallbacks3(String user, String proj) throws IOException {

        ClientInterface c = clients.get(user);
        Project p = getProject(proj);

        if (c != null) {
            c.notifyProj(p, ((WorthProject)p).getChatAddress());
        }
    }


    //notifica tutti i client quando viene cancellato un progetto
    //quale è il progetto cancellato
    public synchronized void doCallbacks4(String proj) throws RemoteException {

        for (String utente : clients.keySet()) {
                clients.get(utente).notifyDelete(proj);
        }
    }


    //metodo per effettuare il login nel server
    public String login(String nickname, String password) throws IOException {

        System.out.println(" Utente " + nickname + " desidera effettuare login");
        String res = "ERROR: utente inesistente, registrarsi e riprovare.";

        if (nickname==null || password==null) return "ERROR: inserire parametri non nulli.";

        //aggiorno la lista degli utenti del sistema
        utenti = files.getUtenti();

        for (User u : utenti) {

            //faccio controlli su nickname e password
            if (u.getNickName().equals(nickname)) {
                if (u.getPassword().equals(password)) {

                    //se i controlli vanno a buon fine aggiungo l'utente alla lista degli utentiOnline
                    if (!utentiOnline.contains(nickname)) {
                        utentiOnline.add(nickname);
                        return "OK";
                    } else return "ERROR: Hai gia' effettuato il login.";
                } else return "ERROR: Password errata. Riprovare.";
            }
        }
        return "ERROR: utente inesistente, registrarsi e riprovare.";
    }

    //metodo per effettuare il logout
    public String logout(String nickname) {

        System.out.println("Utente " + nickname + " desidera effettuare logout.");

        if (nickname==null) return "ERROR: nickname non valido.";

        if (utentiOnline.contains(nickname)) {
            utentiOnline.remove(nickname);
            return "OK";

        } else return "ERROR: effettuare il login e riprovare.";
    }

    //metodo per creare un nuovo progetto
    public String createProject(String projectname, String creatorname) throws IOException {

        if (projectname == null || creatorname == null) return "ERROR: inserire parametri non nulli";

        //controllo che non esista già un progetto con lo stesso nome
        for (Project p : progetti) {
            if (p.getProjectName().equals(projectname))
                return "ERROR: Progetto gia esistente. Scegliere un altro nome e riprovare.";
        }

        //creo il progetto
        Project newProgetto = new WorthProject(projectname, creatorname);

        //aggiorno i file per mantenere la persistenza
        files.aggiornaProgetti(newProgetto);
        //aggiorno la lista di Worth
        this.progetti.add(newProgetto);

        return "OK";
    }

    //metodo per la cancellazione di un progetto
    public String cancelProject(String projectname, String creatorname) throws IOException {

        if (projectname == null) return "ERROR: inserire parametri validi";

        //ottengo il progetto
        Project p = getProject(projectname);

        if (!p.getMembers().contains(creatorname))
                return "ERROR: " + creatorname + " non e' membro del progetto. Non puo' eliminarlo.";

        if(!progetti.contains(p))
                return "ERROR: Progetto non esistente.";

        //controllo se il progetto può essere eliminato , cioè se tutte le card sono nella lista "DONE"
        if (((WorthProject)p).canDelete()) {
            //rimuovo il progetto sia dai file che dalla struttura del server
            progetti.remove(p);
            files.deleteProject(p);
            return "OK";
        } else return "ERROR: tutte le card devono essere completate prima di cancellare il progetto";
    }

    //metodo per aggiungere membro al progetto
    public String addMember(String projectname, String nickname, String adderName) throws IOException {

        if (projectname == null || nickname == null) return "ERROR: inserire parametri non nulli.";

        Project p = getProject(projectname);

        if (p != null) {

            if (!p.getMembers().contains(adderName))
                return "ERROR: utente " + adderName + " non e' membro di " + projectname + ". Non puo' aggiungere un nuovo membro al progetto.";

            if(p.getMembers().contains(nickname))
                return "ERROR: utente " + nickname + " fa gia' parte del team ";

                for (User u : utenti) {

                    if (u.getNickName().equals(nickname)) {
                        p.addProjectMember(nickname);
                        files.aggiornaProgetti(p);

                        return "OK";
                     }
                }
        } else return "ERROR: progetto " + projectname + " inesistente.";

        return "ERROR: utente " + nickname + " inesistente";
    }

    public String showMembers(String projectname, String adderName) {

        if (projectname == null) return "ERROR: inserire parametri non nulli.";

        Project p = getProject(projectname);

        if (!p.getMembers().contains(adderName))
            return "ERROR: utente " + adderName + "non e' membro di " + projectname + ". Non puo' visualizzare membri.";

        if (p != null) {
            //recupero la lista dei membri del progetto p
            List<String> membri = p.getMembers();

            //Se non ci sono membri -- non dovrebbe mai accadere, c'è almeno il creatore
            if (membri == null) return "ERROR: nessun membro.";

            //concateno i membri alla stringa
            StringBuilder out = new StringBuilder();
            for (String i : membri) out.append(i).append(" ");
            return "OK " + out.toString();
        } else {

            return "ERROR: progetto " + projectname + "inesistente.";
        }
    }

    //metodo per ottenere la lista dei progetti dell'utente user
    public String listProjects(String user) {


        StringBuilder str = new StringBuilder();
        // Controllo tutti i progetti di cui è membro
        for (Project i : progetti) {
            //se l'utente user è nel team
            if (i.getMembers().contains(user)) {
                //concateno alla stringa il nome del progetto e uno spazio
                str.append(i.getProjectName());
                str.append(" ");
            }
        }

        //Se la stringa ha lunghezza 0 , non è stato trovato nessun progetto
        if(str.length()!=0) {
            return "OK " + str.toString();
        } else return user + " non partecipa a nessun progetto";
    }


    public String addCard(String projectName, String cardName, String description, String nickname) throws IOException {

        if(projectName == null || cardName == null || description == null) return "ERROR: inserire parametri non null";
        Project tm = getProject(projectName);

        if (!tm.getMembers().contains(nickname))
            return "ERROR: utente " + nickname + "non e' membro di " + projectName + ". Non puo' aggiungere una nuova card al progetto.";

        for(Card c : tm.getCards()) {
            if (c.getName().equalsIgnoreCase(cardName))
                return "ERROR: card gia' presente nel progetto";
        }

        if (tm != null) {
            // Creo la card
            tm.createCard(cardName, description);
            // Aggiorno i file in memoria con la nuova card
            files.aggiornaProgetti(tm);
            return "OK";

        } else return "ERROR: progetto inesistente";

    }

    public String moveCard(String projectName, String cardName, String listaPartenza, String listDestinazione, String nickname) throws IOException {

        if(projectName == null || cardName == null || listaPartenza == null || listDestinazione == null) return "ERROR: inserire parametri non null";

        Project tm = getProject(projectName);

        if (!tm.getMembers().contains(nickname))
            return "ERROR: utente " + nickname + "non e' membro di " + projectName + ". Non puo' effettuare operazioni sulle card.";

        // Se questo esiste
        if (tm != null) {
            // Sposto la card dalla lista di partenza alla lista di destinazione
            if (tm.moveeCard(cardName, listaPartenza, listDestinazione)) {
                //aggiorno i file dei progetti in memoria
                files.aggiornaProgetti(tm);
                return "OK";
            } else {
                return "ERROR: non e' stato possibile spostare la card";
            }

        } else return "ERROR: progetto inesistente";
    }


    //metodo per mostrare le card di un progetto
    public String showCards(String projectName, String utente) {

        Project p = getProject(projectName);

        if (!p.getMembers().contains(utente))
            return "ERROR: utente " + utente + "non e' membro di " + projectName + ". Non puo' visualizzare cards del progetto";

        if (p != null) {
            List<Card> cards = p.getCards();
            if (cards.isEmpty()) return "Nessuna card da mostrare";

            StringBuilder out = new StringBuilder();

            for (Card i : cards) {
                out.append(i.getName());
                out.append(" ");
            }
            return "OK" + out;

        } else return "ERROR: progetto inesistente";
    }

    //metodo per mostrare le caratteristiche di una data card
    public String showCard(String projectName, String cardName, String utente) {

        if(projectName == null || cardName == null) return "ERROR: inserire parametri non null";

        Project p = getProject(projectName);

        if (!p.getMembers().contains(utente))
            return "ERROR: utente " + utente + "non e' membro di " + projectName + ". Non puo' visualizzare i dettagli della card.";

        if (p != null) {

            Card card = p.getCard(cardName);

            if (card != null) {
                return "OK Nome:" + card.getName() + "\n " + "Descrizione: " + card.getDescription() + "\n" + "Stato: " + card.getState();
            } else return "ERROR: card inesistente";
        } else return "ERROR: progetto inesistente";

    }


    public String cardHistory(String projectName, String cardName, String utente) {

        if(projectName == null || cardName == null) return "ERROR: inserire parametri non null";


        Project p = getProject(projectName);

        if (!p.getMembers().contains(utente))
            return "ERROR: utente " + utente + "non e' membro di " + projectName + ". Non puo' visualizzare la history della card.";

        Card card = p.getCard(cardName);

        if (p != null) {
            if (card != null) {
                return "OK" + card.getHistory();
            } else return "ERROR: card inesistente";

        } else return "ERROR: progetto inesistente";
    }


}






