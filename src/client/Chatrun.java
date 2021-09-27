package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

public class Chatrun implements Runnable {

        private InetAddress address;
        private String nomeProgetto;
        private ArrayList<String> messaggiChat;
        private MulticastSocket multicastSocket;

        public Chatrun(InetAddress address, String nomeProgetto) {

            this.address = address;
            this.nomeProgetto = nomeProgetto;
            messaggiChat = new ArrayList<>();
        }

        public void setMulticastSocket(MulticastSocket ms) {
            this.multicastSocket = ms;
        }


        // Stampa i messaggi ricevuti dal momento del login la prima volta che viene eseguito
        // in seguito stampa i messaggi ricevuti dall'ultima esecuzione
        public void getMessaggiChat() {

            ArrayList<String> newMessaggi = new ArrayList<>();
            for (int i = 0; i < messaggiChat.size(); i++) {
                String messaggio = messaggiChat.get(i);
                newMessaggi.add(messaggio);
                System.out.println(messaggio);
            }
            if(messaggiChat.isEmpty()) {
                System.out.println("Non ci sono messaggi");
            }
            messaggiChat.clear();
        }

        // Invia un messaggio alla Chat del progetto
        public void inviaMessaggio(String utente, String messaggio) {

            String tosend = utente + ": " + messaggio;

            try {
                DatagramPacket datagramPacket = new DatagramPacket(tosend.getBytes(), tosend.length(), this.address, 7777);
                multicastSocket.send(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (utente != "Server")
                System.out.println("Messaggio inviato!");
        }

        // Chiude la MulticastSocket associata alla Chat
        public void closeChat() {

            if (multicastSocket != null) {

                if (!multicastSocket.isClosed()) {
                    multicastSocket.close();
                }
                multicastSocket.disconnect();
                multicastSocket.close();
            }
        }

        @Override
        public void run() {

            while (true) {

                if (multicastSocket.isClosed())
                    break;

                byte[] buf = new byte[512];

                try {
                    DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
                    multicastSocket.receive(msgPacket);
                    String msg = new String(msgPacket.getData());
                    messaggiChat.add(msg);
                } catch (IOException e) {

                }
            }
        }
    }

