package client;

import server.Project;
import server.RMIServer;
import server.WorthProject;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class WorthClient extends RemoteObject implements ClientInterface {

    private String username; //username dell'utente loggato nel client
    private boolean loggedIn; //flag settato a false di default, viene impostato a true quando l'utente effettua il login
    private Map<String, String> users; //mappa nickname-stato
    private Map<Project, String> projects; //mappa progetto-indirizzoIP

    private Map<Project,Chatrun> chatThreads; //mappa progetto - oggettoChat

    Registry registry;
    RMIServer RMIServer;
    ClientInterface callStub;

    private SocketChannel client;


    public WorthClient() {

        super();

        username = null;
        loggedIn = false;

        this.chatThreads = new HashMap<>();
        this.users = new HashMap<>();
        this.projects = new HashMap<>();

    }

    public void start() {

        try {
            // INIZIALIZZAZIONE SERVIZI RMI
            try {
                registry = LocateRegistry.getRegistry(6789);
                RMIServer = (RMIServer) registry.lookup("WORTH");
                callStub = (ClientInterface) UnicastRemoteObject.exportObject(this, 0);
            } catch (NotBoundException e) {
                e.printStackTrace();
                return;
            }

            //INIZIALIZZAZIONE CONNESSIONE TCP
            try {
                 client = SocketChannel.open();
                 client.connect(new InetSocketAddress(9999));
            } catch (ClosedChannelException e) {
                e.printStackTrace();
                return;
            }

            boolean end = true;

            System.out.println("****BENVENUTO IN WORTH****");
            System.out.println("Per scoprire i comandi digita help");

            while (end) {

                Scanner scanner = new Scanner(System.in);
                String next = scanner.nextLine();

                StringTokenizer tokenizer = new StringTokenizer(next);
                List<String> comandi = new ArrayList<>();

                while (tokenizer.hasMoreElements()) comandi.add(tokenizer.nextToken());

                switch (comandi.get(0).toLowerCase()) {

                    case "help":
                        showHelp();
                        break;

                    case "register":
                        if (loggedIn) {
                            System.out.println("ERROR: Un utente e' gia' loggato. impossibile registrarsi");
                            break;
                        }
                        regServer(comandi);
                        break;

                    case "login": {
                        if (loggedIn) {
                            System.out.println("ERROR:  utente gia' loggato. Effettuare logout e riprovare");
                            break;
                        }

                        logServer(comandi);
                        break;
                    }

                    case "logout":
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima effettuare il login");
                            break;
                        }

                        logoutServer(comandi);
                        break;


                    case "listusers": {
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima effettuare login");
                            break;
                        }

                        listUsers(comandi);
                        break;
                    }

                    case "onlineusers":
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima effettuare login");
                            break;
                        }

                        onlineusers(comandi);
                        break;

                    case "createproject":
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima efettuare il login");
                            break;
                        }

                        createProject(comandi);
                        break;

                    case "addmember":
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima efettuare il login");
                            break;
                        }

                        addmember(comandi);
                        break;

                    case "showmembers":
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima effettuare il login");
                            break;
                        }

                        showmembers(comandi);
                        break;

                    case "listprojects":
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima effettuare il login");
                            break;
                        }

                        listprojects(comandi);
                        break;

                    case "addcard":
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima effettuare il login");
                            break;
                        }

                        addcard(comandi);
                        break;

                    case "movecard":
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima effettuare il login");
                            break;
                        }

                        moveCard(comandi);
                        break;

                    case "showcards":
                        if(!loggedIn){
                            System.out.println("ERROR: devi prima effettuare il login");
                            break;
                        }

                        showCards(comandi);
                        break;

                    case "showcard":
                        if(!loggedIn){
                            System.out.println("ERROR: devi prima effettuare il login");
                            break;
                        }

                        showCard(comandi);
                        break;

                    case "cardhistory":
                        if(!loggedIn){
                            System.out.println("ERROR: devi prima effettuare il login");
                            break;
                        }

                        cardHistory(comandi);
                        break;

                    case "readchat":
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima effettuare il login");
                            break;
                        }

                        readChat(comandi);
                        break;

                    case "sendchat":
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima effettuare il login");
                            break;
                        }

                        sendChat(comandi);
                        break;


                    case "cancelproject":
                        if (!loggedIn) {
                            System.out.println("ERROR: devi prima effettuare il login");
                            break;
                        }

                        cancelProject(comandi);
                        break;

                    case "exit":
                        if (loggedIn) {
                            System.out.println("Per terminare il client è necessario effettuare il logout.");
                            break;
                        }

                        exit(comandi);
                        end = false;
                        break;


                    default:
                        System.out.println("Comando inesistente: digitare help per conoscere i comandi disponibili");
                        break;
                }

            }

            System.out.println("Client arrestato.");
            System.exit(0);

        } catch (ConnectException e) {
            System.out.println("Server irraggiungibile, arresto in corso");
            System.exit(0);
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("Server irraggiungibile, arresto in corso");

            System.exit(0);
        } catch (IOException e) {
            System.out.println("Server irraggiungibile, arresto in corso");
            System.exit(0);
        }
    }

    private void cardHistory(List<String> comandi) {

        if (comandi.size() != 3) {
            System.out.println("DIGITARE: cardhistory nomeprogetto nomecard");
            return;
        }

        try {

            String send = comandi.get(0) + " " + comandi.get(1) + " " + comandi.get(2) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb); //scrive il buffer sul channel

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb); //legge dal canale sul buffer

            //flip prima della decodifica per portare il buffer nello stato
            //che voglio venga letto dalla decodifica
            //la position è all'inizio
            bb.flip();

            //decodifico il bytebuffer e lo converto in stringa
            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            if (res.startsWith("OK")) {
                System.out.println("History card " + comandi.get(2) + ":");
                System.out.println(res.substring(2, res.length()));

            } else {
                System.out.println(res);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }



    private void showCard(List<String> comandi) {

        if (comandi.size() != 3) {
            System.out.println("DIGITARE: showcard nomeprogetto nomecard");
            return;
        }

        try {

            String send = comandi.get(0) + " " + comandi.get(1) + " " + comandi.get(2) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb);

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb);

            bb.flip();

            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            if (res.startsWith("OK")) {
                System.out.println("Informazioni relative alla card" + comandi.get(2) + ":");
                System.out.println(res.substring(2, res.length()));
            } else {
                System.out.println("ERROR: nessuna card associata al progetto");
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }



    private void showCards(List<String> comandi) {

        if (comandi.size() != 2) {
            System.out.println("DIGITARE: showcards nomeprogetto");
            return;
        }

        try {

            String send = comandi.get(0) + " " + comandi.get(1) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb);

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb);

            bb.flip();

            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            if (res.startsWith("OK")) {

                System.out.println("Cards del progetto " + comandi.get(1) + ":");
                System.out.println(res.substring(2, res.length()));

            } else {
                System.out.println(res);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private void exit(List<String> comandi) {

        try {

            String send = comandi.get(0) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb);

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb);

            bb.flip();

            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            client.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private void cancelProject(List<String> comandi) {

        if (comandi.size() != 2) {
            System.out.println("DIGITARE: cancelproject nomeprogetto");
            return;
        }

        try {

            String send = comandi.get(0) + " " + comandi.get(1) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb);

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb);

            bb.flip();

            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            if (res.startsWith("OK")) {
                System.out.println("Progetto " + comandi.get(1) + " cancellato");
            } else {
                System.out.println(res);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    private void sendChat(List<String> comandi) {

        if (comandi.size() < 3) {
            System.out.println("DIGITARE: sendchat projectname messaggio");
            return;
        }

        boolean found = false;

        for (Project p : projects.keySet()) {
            if (p.getProjectName().equals(comandi.get(1))) {
                found = true;
            }
        }

        if (!found) {
            System.out.println("ERROR: impossibile accedere alla chat");
        } else {

            StringBuilder messaggio = new StringBuilder();

            for (int i = 2; i < comandi.size(); i++) {
                messaggio.append(comandi.get(i)).append(" ");
            }

            for (Project p : chatThreads.keySet()) {
                if (p.getProjectName().equals(comandi.get(1))) {

                    Chatrun chat = chatThreads.get(p);

                    if (chat != null) {
                        chat.inviaMessaggio(username, messaggio.toString());
                    }
                }
            }
        }
    }


    private void readChat(List<String> comandi) {

            if (comandi.size() != 2) {
                System.out.println("DIGITARE: readchat nomeprogetto");
                return;
            }

            boolean found = false;

            for (Project p : projects.keySet()) {

                if (p.getProjectName().equals(comandi.get(1))) {
                    found = true;
                }
            }

            if (!found) {
                System.out.println("ERROR: impossibile accedere a questa chat");
            } else {

                for (Project p : chatThreads.keySet()) {

                    if (p.getProjectName().equals(comandi.get(1))) {
                        Chatrun chat = chatThreads.get(p);

                        if (chat != null) chat.getMessaggiChat();
                    }
                }
            }
        }


        public void regServer(List<String> comandi) {

        String res = "";

        if (comandi.size() != 3) {
            System.out.println("ERRORE: digitare register nickname password");
            return;
        }

        try {
            res = RMIServer.register(comandi.get(1), comandi.get(2));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (res.startsWith("OK")) {
            System.out.println("Registrazione avvenuta. Benvenuto in WORTH " + comandi.get(1));
        } else
            System.out.println(res);

    }

    public void logServer(List<String> comandi) {

        if (comandi.size() != 3) {
            System.out.println("ERRORE: digitare login nickname password");
            return;
        }

        try {
            //registro il client tra gli utenti disponibili alla callback
            RMIServer.registerForCallback(callStub, comandi.get(1));
        } catch(RemoteException e) {
            e.printStackTrace();
        }

        try {

            String send = comandi.get(0) + " " + comandi.get(1) + " " + comandi.get(2);
            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb);

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);

            client.read(bb);

            bb.flip();

            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();

                if (res.startsWith("OK")) {

                    System.out.println("Login effettuato. Bentornato " + comandi.get(1));

                    this.username = comandi.get(1);
                    this.loggedIn = true;

                    //per ogni progetto viene avviato il thread chat e aggiornata la struttura dati
                    for(Project p : projects.keySet()) {

                            if(!chatThreads.containsKey(p)) {

                                InetAddress ia = InetAddress.getByName(((WorthProject)p).getChatAddress());
                                MulticastSocket socket = new MulticastSocket(7777);
                                socket.joinGroup(ia);

                                Chatrun newChat = new Chatrun(ia, p.getProjectName());
                                newChat.setMulticastSocket(socket);
                                new Thread((newChat)).start();
                                chatThreads.putIfAbsent(p,newChat);
                            }
                    }

                } else {
                    //se il login non avviene correttamente lo rimuovo dagli utenti disponibili per callback
                    RMIServer.unregisterForCallback(comandi.get(1));
                    System.out.println(res);
                }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void logoutServer(List<String> comandi) {

        if (comandi.size() != 1) {
            System.out.println("DIGITARE: logout");
            return;
        }

        try {

            String send = comandi.get(0) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb);

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb);

            bb.flip();

            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            if (res.startsWith("OK")) {
                //se il logout viene effettuato con successo rimuovo il client dagli utenti disponibili per la callback
                RMIServer.unregisterForCallback(this.username);

                System.out.println("Logout effettuato. Arrivederci " + this.username);

                this.username = null;
                this.loggedIn = false;

                //al logout tutti i thread delle chat vengono interrotti
                for (Project p : chatThreads.keySet()) {
                    chatThreads.get(p).closeChat();
                }
                //e la struttura svuotata- verrà nuovamente riempita al login
                chatThreads.clear();
            } else {
                System.out.println(res);
            }

        } catch (IOException e) {
        }

    }


    public void listUsers(List<String> comandi) {

        if (comandi.size() != 1)  {
            System.out.println("DIGITARE: listusers");
            return;
        }

        System.out.println("Lista utenti Worth -> stato attuale: ");
        for (String u : users.keySet()) {
            System.out.println(u + " -> " + users.get(u));
        }
    }

    public void onlineusers(List<String> comandi) {

        if (comandi.size() != 1) {
            System.out.println("ERROR: digitare onlineusers");
            return;
        }

        StringBuilder out = new StringBuilder();

        for (String u : users.keySet()) {
            if (users.get(u).equals("online")) {
                out.append(u).append( " ");
            }
        }

        System.out.println("Lista utenti attualmente online:");
        System.out.println(out);
    }

    public void createProject(List<String> comandi) {

        if (comandi.size() != 2) {
            System.out.println("DIGITARE: createproject nomeprogetto");
            return;
        }

        try {

            String send = comandi.get(0) + " " + comandi.get(1) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb);

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb);

            bb.flip();

            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            if (res.startsWith("OK")) {

                System.out.println("Progetto " + comandi.get(1) + " creato");

            } else {
                System.out.println(res);
            }


        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public void addmember(List<String> comandi) {

        if (comandi.size() != 3) {
            System.out.println("DIGITARE: addmember nomeprogetto nickname");
            return;
        }

        try {

            String send = comandi.get(0) + " " + comandi.get(1) + " " + comandi.get(2) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb);

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb);

            bb.flip();

            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            if (res.startsWith("OK")) {
                System.out.println("OK: membro " + comandi.get(2) + "aggiunto da " + this.username + "al progetto " + comandi.get(1) + ".");

            } else {
                System.out.println(res);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }


    public void showmembers(List<String> comandi) {

        if (comandi.size() != 2) {
            System.out.println("DIGITARE: showmembers nomeprogetto");
            return;
        }

        try {

            String send = comandi.get(0) + " " + comandi.get(1) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb);

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb);

            bb.flip();

            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            if (res.startsWith("OK")) {
                System.out.println(res.substring(3, res.length()));
            } else {
                System.out.println(res);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void listprojects(List<String> comandi) {

        if (comandi.size() != 1) {
            System.out.println("ERROR: digitare listprojects");
            return;
        }

        try {

            String send = comandi.get(0) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb); //scrive il buffer nel canale

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb);

            //flip prima della decodifica per portare il buffer nello stato
            //che voglio venga letto dalla decodifica
            //la nnext position è all'inizio
            bb.flip();

            //decodifico il bytebuffer e lo converto in stringa
            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            if (res.startsWith("OK")) {
                System.out.println(res.substring(3, res.length()));
            } else {
                System.out.println(res);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public void addcard (List<String> comandi) {

        if (comandi.size() != 4) {
            System.out.println("DIGITARE: addcard nomeprogetto nomecard descrizione");
            return;
        }

        try {

            String send = comandi.get(0) + " " + comandi.get(1) + " " + comandi.get(2) + " " + comandi.get(3) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());

            client.write(bb);

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb);

            bb.flip();

            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            if (res.startsWith("OK")) {
                System.out.println("Card " + comandi.get(2) + " aggiunta");
            } else {
                System.out.println(res);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public void moveCard(List<String> comandi) {

        if (comandi.size() != 5) {
            System.out.println("ERRORE. Per spostare una card digitare : movecard nomeprogetto nomecard listapartenza listadestinazione");
            return;
        }

        try {

            String send = comandi.get(0) + " " + comandi.get(1) + " " + comandi.get(2) + " " + comandi.get(3) + " " + comandi.get(4) + " " + this.username;

            ByteBuffer bb = ByteBuffer.wrap(send.getBytes());
            client.write(bb);

            bb.clear();
            bb.flip();

            bb = ByteBuffer.allocate(1024);
            client.read(bb);

            bb.flip();

            String res = StandardCharsets.US_ASCII.decode(bb).toString();
            bb.clear();
            bb.flip();

            if (res.startsWith("OK")) {
                System.out.println("Card " + comandi.get(2) + "spostata");

                for (Project p : projects.keySet()) {
                    if (p.getProjectName().equals(comandi.get(1))) {

                        Chatrun chat = chatThreads.get(p);

                        if (chat != null) {
                            //se la card viene spostata il server invia messaggio in chat che notifica lo spostamento
                            chat.inviaMessaggio("Server", "Card " + comandi.get(2) + "spostata: "  + comandi.get(3) + "->" + comandi.get(4));
                        }
                    }
                }
            } else {
                System.out.println(res);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }


    //map utenti-stato aggiorata tramite callback dopo aver effettuato il login
    @Override
    public void notifyEvent(Map<String, String> usrs) throws RemoteException {
        this.users = usrs;
    }


    //aggiornamento map progetto-indirizzo e map progetto-chat
    //viene mandata in esecuzione la chat del progetto inviato
    @Override
    public void notifyProj(Project p, String add) throws RemoteException,IOException {

        if(!projects.containsKey(p)) projects.putIfAbsent(p,add);

        if(!chatThreads.containsKey(p)) {
            InetAddress ia = InetAddress.getByName(add);
            MulticastSocket socket = new MulticastSocket(7777);
            socket.joinGroup(ia);

            Chatrun newChat = new Chatrun(ia, p.getProjectName());
            newChat.setMulticastSocket(socket);
            new Thread((newChat)).start();
            chatThreads.putIfAbsent(p,newChat);

        }
    }

    //aggiornamento progetto eliminato
    //viene eliminato dalle strutture del client e viene interrotta la relativa chat
    @Override
    public void notifyDelete(String p) throws RemoteException {

        for(Project proj : chatThreads.keySet()) {
            if(proj.getProjectName().equals(p)) {
                chatThreads.remove(p);

                if(projects.containsKey(proj)){
                    projects.remove(proj);
                }
            }
        }
    }

    //aggiornamento map progetto-indirizzo tramite callback
    @Override
    public void notifySockets(Map<Project, String> sock) {
        this.projects=sock;
    }


    private void showHelp() {
        System.out.println("ELENCO DELLE FUNZIONI DISPONIBILI:"  );
        System.out.println( "register [nickname] [password] -> effettua la registrazione" );
        System.out.println("login [nickname] [password] -> effettua il login " );
        System.out.println("logout -> effettua il logout dell'utente loggato");
        System.out.println( "listprojects -> mostra i progetti a cui stai lavorando");
        System.out.println( "createproject [nomeprog] -> crea un progetto");
        System.out.println( "addmember [nomeprog] [nickname] -> aggiungi un membro ad un progetto");
        System.out.println( "showmembers [nomeprog] -> visualizza i membri di un progetto");
        System.out.println( "showcards [nomeprog] -> mostra i taks (Cards) di un progetto");
        System.out.println( "listusers -> visualizza l'elenco degli utenti resistrati su Worth e il loro stato");
        System.out.println( "onlineusers  -> visualizza chi è online in questo momento");
        System.out.println( "showcard [nomeprog] [cardname]  -> visualizza il dettaglio di una Card");
        System.out.println( "addcard [nomeprog] [cardname] [descr]  -> aggiungi una card ad un progetto");
        System.out.println( "movecard [nomeprog] [cardname] [list1] [list2] -> sposta una card da uno stato di lavorazione ad un altro");
        System.out.println( "cardhistory [nomeprog] [cardname]  -> visualizza lo storico di lavorazione di una card");
        System.out.println( "readchat [nomeprog] -> visualizza i messaggi non letti nella chat del progetto");
        System.out.println( "sendchatmsg [nomeprog] [message]  -> invia un messaggio agli altri membri del progetto"); //sendchat*
        System.out.println("cancelproject [nomeprog]  -> elimina un progetto");
        System.out.println( "exit  -> termina il client");
        System.out.println( "help  -> rivedere questa schermata");
    }

}
