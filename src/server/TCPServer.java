package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class TCPServer implements Runnable {

    private final WorthServer server;

    public TCPServer(WorthServer server) {
        this.server = server;
    }

    public void run() {

        ServerSocketChannel serverSocket;
        Selector selector;

        try {

            // Apro il ServerSocketChannel,
            // la porta a cui offrire il servizio definisco il selector,
            // con la preimpostazione su accept (per accettare nuove connessioni)
            serverSocket = ServerSocketChannel.open();
            serverSocket.socket().bind(new InetSocketAddress(9999));
            serverSocket.configureBlocking(false);
            selector = Selector.open();

            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server TCP avviato alla porta 9999.");


        } catch (IOException e) {
            //System.out.println("Server 9998.");

            e.printStackTrace();
            return;
        }

        while (true) {

            try {
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            //itero le key per verificare se sono accettabili, scrivibili o leggibili
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();

            while (iterator.hasNext()) {

                SelectionKey key = iterator.next();
               /* il metodo remove() elimina dall'insieme l'elemento corrente,
               cioè l'elemento restituito dall'ultima invocazione di next() precedente il remove().*/
                iterator.remove();

                try {
                    //Accetta connesioni dal client

                    //la key è la chiave di selezione associata al serverChannel
                    //se la key è acceptable significa che il canale sottostante è pronto per
                    //accettare la richiesta di connessione di un client
                    if (key.isAcceptable()) {

                        ServerSocketChannel server = (ServerSocketChannel) key.channel();

                        try {

                            //apro la connessione
                            //prendo il socketchannel che sta provando a connettersi
                            SocketChannel client = server.accept();
                            client.configureBlocking(false);

                            //registro la key su readable
                            client.register(selector, SelectionKey.OP_READ);
                            //informo che ho accettato una nuova connessione
                            System.out.println("accettata nuova connessione dal client");
                        } catch (IOException e) {
                            server.close();
                            return;
                        }

                        //canale assocciato alla chiave pronto per la scrittura
                    } else if (key.isWritable()) {

                        SocketChannel client = (SocketChannel) key.channel();
                        /*Ciò che deve essere scritto è contenuto
                        nell'attachment alla chiave, in forma di stringa
                        L'attachment viene scritto sul canale */

                        String toClient = (String) key.attachment();

                        if (toClient != null) {

                            ByteBuffer buffer = ByteBuffer.wrap(toClient.getBytes());
                            //write(bytebuffer) preleva i byte dal buffer : li legge
                            int esitoWrite = client.write(buffer);

                            if (esitoWrite == -1) { //0
                                key.cancel();       //cancello la key
                                key.channel().close(); //e chiudo la connessione
                            } else if (buffer.hasRemaining()) { //se non tutti i byte sono stati inviati lungo il canale
                                buffer.flip();
                                String messaggio = StandardCharsets.US_ASCII.decode(buffer).toString();
                                key.attach(messaggio);
                            } else {
                                key.attach(null);
                                key.interestOps(SelectionKey.OP_READ); //rendo la key leggibile
                            }
                        }
                        //canale corrispondente alla chiave pronto per la lettura
                    } else if (key.isReadable()) {

                        SocketChannel client = (SocketChannel) key.channel();

                        //buffer per la lettura non bloccante da canale
                        ByteBuffer bb = ByteBuffer.allocate(1024);

                        bb.clear();
                        int esitoRead = client.read(bb);

                        if (esitoRead == -1) {
                            key.cancel();
                            client.close();
                        } else {

                            //bytebuffer.array() restituisce l'array di byte del buffer
                            String risposta = new String(bb.array()).trim();
                            String[] split = risposta.split(" ");
                            String operazione = split[0];
                            String b;

                            if (operazione.equals("login")) {

                                System.out.println("Richiesta di Login");
                                String username = split[1];
                                String password = split[2];
                                b = server.login(username, password);

                                if (b.equals("OK")) {
                                    server.doCallBacks();
                                    server.doCallBacks2(username);
                                }
                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);

                            }

                            if (operazione.equals("logout")) {

                                System.out.println("Richiesta di Logout");
                                String username = split[1];
                                b = server.logout(username);

                                if (b.equals("OK")) {
                                    server.doCallBacks();
                                }
                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);
                            }

                            if (operazione.equals("createproject")) {
                                System.out.println("Richiesta di creazione progetto");

                                String projectname = split[1];
                                String creator = split[2];
                                b = server.createProject(projectname, creator);

                                if (b.startsWith("OK")) {
                                    server.doCallbacks3(creator, projectname);
                                }
                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);
                            }

                            if (operazione.equals("addmember")) {

                                System.out.println("Richiesta di aggiungere un membro a un progetto");

                                String projectname = split[1];
                                String toAdd = split[2];
                                String adder = split[3];

                                b = server.addMember(projectname, toAdd, adder);

                                if (b.startsWith("OK")) {
                                    server.doCallbacks3(toAdd, projectname);
                                }
                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);
                            }

                            if (operazione.equals("showmembers")) {

                                System.out.println("Richiesta di visualizzare membro progetto");
                                String projectname = split[1];
                                String adder = split[2];

                                b = server.showMembers(projectname, adder);

                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);

                            }

                            if (operazione.equals("listprojects")) {

                                System.out.println("Richiesta di visualizzare lista progetti");

                                String username = split[1];

                                b = server.listProjects(username);

                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);
                            }

                            if (operazione.equals("addcard")) {

                                System.out.println("Richiesta di aggiungere carta ad un progetto");
                                String projectname = split[1];
                                String cardname = split[2];
                                String descrizione = split[3];
                                String user = split[4];

                                b = server.addCard(projectname, cardname, descrizione,user);

                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);

                            }

                            if (operazione.equals("movecard")) {

                                System.out.println("Richiesta di muovere card");
                                String projectname = split[1];
                                String cardname = split[2];
                                String listapart = split[3];
                                String listadest = split[4];
                                String utente = split[5];

                                b = server.moveCard(projectname, cardname, listapart, listadest,utente);

                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);
                            }

                            if (operazione.equals("showcards")) {

                                System.out.println("Richiesta di vedere card progetto");
                                String projectname = split[1];

                                String utente = split[2];

                                b = server.showCards(projectname,utente);

                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);
                            }

                            if (operazione.equals("showcard")) {

                                System.out.println("Richiesta di vedere info card ");
                                String projectname = split[1];
                                String cardname = split[2];

                                String utente = split[3];

                                b = server.showCard(projectname,cardname,utente);

                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);
                            }

                            if (operazione.equals("cardhistory")) {

                                System.out.println("Richiesta di history carta ");
                                String projectname = split[1];
                                String cardname = split[2];
                                String user = split[3];

                                b = server.cardHistory(projectname, cardname,user);

                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);

                            }

                            if (operazione.equals("cancelproject")) {
                                System.out.println("Richiesta di cancellazione progetto");

                                String projectname = split[1];
                                String creator = split[2];
                                b = server.cancelProject(projectname, creator);

                                if(b.startsWith("OK")) {
                                    server.doCallbacks4(projectname);
                                }

                                key.attach(b);
                                key.interestOps(SelectionKey.OP_WRITE);
                            }

                            if (operazione.equals("exit")) {
                                System.out.println("Richiesta di disconessione client");

                                key.channel().close();
                                key.cancel();

                            }
                        }
                    }

                } catch (IOException e) {
                    key.cancel();

                    try {
                        key.channel().close();
                    } catch (IOException ex) {
                       ex.printStackTrace();
                    }
                }
            }
        }

    }
}



