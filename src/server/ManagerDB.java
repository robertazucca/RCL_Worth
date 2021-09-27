package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.System.exit;

public class ManagerDB {

    private final File db;
    private final File users;
    private final File projects;
    private final ObjectMapper objectMapper;

    public ManagerDB() {

        System.out.println("Server data initialization - start");

        this.objectMapper = new ObjectMapper();

        this.db = new File("./Database");
        this.users = new File("./Database/Users");
        this.projects = new File("./Database/Projects");

        if (!existsServerPath()) {
            if (!createServerPath()) {
                System.out.println("Server Path Create Error");
                exit(0);
            }
        }
        System.out.println("Server data initialization - finish");
    }

    private boolean existsServerPath() {
        return this.db.exists();
    }

    // Metodo per creare la cartella server
    private boolean createServerPath() {
        return this.db.mkdir() && createUsersPath() && createProjectsPath();
    }

    // Metodo per creare la cartella Users (this.serverPath + "/Users")
    private boolean createUsersPath() {
        return this.users.mkdir();
    }

    private boolean createProjectsPath() {
        return this.projects.mkdir();
    }

    private void createProjectCardsPath(File cards) {
        if (!cards.exists()) {
            boolean dir = cards.mkdir();
        }
    }


    public List<User> getUtenti() throws IOException {

        List<User> listaUtenti = new CopyOnWriteArrayList<>();

        if (this.users.isDirectory()) {

            String[] filesUtenti = this.users.list();

            if (filesUtenti != null) {
                Arrays.sort(filesUtenti);

                File FileUtente;

                for (String nomeUtente : filesUtenti) {
                    FileUtente = new File(users + "/" + nomeUtente);
                    listaUtenti.add(objectMapper.readValue(FileUtente, WorthUser.class));
                }
            }
        }
        return listaUtenti;
    }

    public boolean aggiornaUtenti(User user) throws IOException {

        File FileUtente = new File(users + "/" + user.getNickName() + ".json");

        if (FileUtente.createNewFile()) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FileUtente, user);
            return true;
        }
        return false;
    }


    public void aggiornaProgetti(Project project) throws IOException {

        File projectFile = new File(projects + "/" + project.getProjectName() + ".json");
        File cards = new File(this.projects + "/" + project.getProjectName());

        if (!projectFile.exists()) if (projectFile.createNewFile()) createProjectCardsPath(cards);

        File currentCardFile;
        for (Card currentCard : project.getCards()) {

            currentCardFile = new File(cards + "/" + currentCard.getName() + ".json");

            if (!currentCardFile.exists()) {
                boolean newFile = currentCardFile.createNewFile();
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(currentCardFile, currentCard);
        }

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(projectFile, project);

    }


    public List<Project> getProjects() throws IOException {

        List<Project> listaprog = new CopyOnWriteArrayList<>();

        if (this.projects.isDirectory()) {

            String[] filesProgetti = this.projects.list();

            if (filesProgetti != null) {

                File projectFile;
                File cartellaCards;
                Project progettoCorrente;

                List<Card> carteProgCorrente = new ArrayList<>();

                for (String nomeProgetto : filesProgetti) {

                    projectFile = new File(projects + "/" + nomeProgetto);

                    if (projectFile.isDirectory()) continue;

                    progettoCorrente = objectMapper.readValue(projectFile, WorthProject.class);
                    cartellaCards = new File(projects + "/" + progettoCorrente.getProjectName());

                    if (cartellaCards.isDirectory()) {
                        String[] cardsPaths = cartellaCards.list();
                        if (cardsPaths != null) {
                            File currentCard;
                            for (String cardName : cardsPaths) {
                                currentCard = new File(cartellaCards + "/" + cardName);
                                carteProgCorrente.add(objectMapper.readValue(currentCard, Card.class));
                            }
                        }
                    }

                    //al ripristino dei progetti viene assegnato un indirizzo
                    //((WorthProject)progettoCorrente).setChatAddress(MulticastAddressGenerator.getAddress());
                    ((WorthProject) progettoCorrente).setChatAddress(MulticastAddressGenerator.getAddress());
                    progettoCorrente.addCards(carteProgCorrente);
                    carteProgCorrente.clear();

                    listaprog.add(progettoCorrente);
                }
            }
        }

        return listaprog;
    }

    public void deleteProject(Project p) throws IOException {

        File fileProgetto = new File(projects + "/" + p.getProjectName() + ".json");

        File cards = new File(this.projects + "/" + p.getProjectName());

        //riciclo dell'indirizzo, può essere usato per altri progetti
        MulticastAddressGenerator.freeAddress(((WorthProject)p).getChatAddress());

        if (cards.exists() && cards.isDirectory()) {

            File[] files = cards.listFiles();
            for (File e : files) {
                e.delete();
            }

            cards.delete();
        }

        fileProgetto.delete();
    }
}


