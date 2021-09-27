package server;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class WorthProject implements Project, Serializable {

    private String projectName;
    private String creator;
    private List<String> members;

    private List<Card> TODO;
    private List<Card> INPROGRESS;
    private List<Card> TOBEREVISITED;
    private List<Card> DONE;

    @JsonIgnore
    private String chatAddress;

    public WorthProject(String projectName, String creator) {

        if (projectName == null || creator == null) throw new NullPointerException("ProjectImpl - Invalid Parameters");
        this.projectName = projectName;
        this.creator = creator;

        // Inizializzo e aggiungo il creatore alla lista dei membri
        this.members = new ArrayList<>();
        this.members.add(creator);

        //alla creazione del progetto viene assegnato l'indirizzo multicast per la chat
        this.chatAddress = MulticastAddressGenerator.getAddress();

        // Inizializzo le varie liste
        this.TODO = new ArrayList<>();
        this.INPROGRESS = new ArrayList<>();
        this.TOBEREVISITED = new ArrayList<>();
        this.DONE = new ArrayList<>();
    }

    //Costruttore per jackson
    public WorthProject() {
        // Inizializzo tutti gli argomenti
        this.projectName = null;
        this.creator = null;
        this.members = new ArrayList<>();

        this.TODO = new ArrayList<>();
        this.INPROGRESS = new ArrayList<>();
        this.TOBEREVISITED = new ArrayList<>();
        this.DONE = new ArrayList<>();

    }

    public String getProjectName() {
        return this.projectName;
    }

    public String getCreator() { return this.creator; }

    public List<String> getMembers() { return this.members; }

    public void addProjectMember(String name) {
        //non controllo se è null perchè faccio già i controlli nel server, se è null non arrivo a chiamare addprojectname
        // non Verifico se il membro fa già parte del team, faccio in worth
        // Aggiungo l'utente ai membri
        this.members.add(name);
    }


    public void addCards(List<Card> cards) {

        for(Card c : cards) {

            String cstate = c.getState();

            switch(cstate) {
                case "TODO" :
                    TODO.add(c);
                    break;
                case "INPROGRESS" :
                    INPROGRESS.add(c);
                    break;
                case "TOBEREVISITED" :
                    TOBEREVISITED.add(c);
                    break;
                case "DONE":
                    DONE.add(c);
                    break;
            }
        }
    }

    public void createCard(String name, String description)  {
        // Creo la card e la aggiungo alla lista TOD0
        Card tmp = new Card(name, description);
        this.TODO.add(tmp);
    }

    @JsonIgnore
    // Metodo per ritornare tutte le card presenti nel progetto
    public List<Card> getCards(){
        // Inizializzo una lista
        List<Card> t = new ArrayList<>();
        // Aggiungo tutte le card di tutte le liste
        t.addAll(TODO);
        t.addAll(INPROGRESS);
        t.addAll(TOBEREVISITED);
        t.addAll(DONE);
        return t;
    }

    public Card getCard(String name) {
        // Verifico il parametro
        //se è null non viene mai chiamata getCard
        //if (name == null) throw new NullPointerException("getCard - Invalid Parameter");
        // Cerco la card in tutte le liste finchè non la trovo, se non esiste Exception
        for (Card i : TODO) if (i.getName().equals(name)) return i;
        for (Card i: INPROGRESS) if(i.getName().equals(name)) return i;
        for (Card i: TOBEREVISITED) if(i.getName().equals(name)) return i;
        for (Card i: DONE) if(i.getName().equals(name)) return i;

        return null;
    }

    public boolean moveeCard(String card, String listainiz, String listadest) {

        Card c = getCard(card);

        if(listainiz.equalsIgnoreCase("TODO") && listadest.equalsIgnoreCase("INPROGRESS")) {
            INPROGRESS.add(c);
            TODO.remove(c);
            c.update("INPROGRESS");
            return true;
        }

        if(listainiz.equalsIgnoreCase("INPROGRESS") && listadest.equalsIgnoreCase("TOBEREVISITED")) {
            TOBEREVISITED.add(c);
            INPROGRESS.remove(c);
            c.update("TOBEREVISITED");
            return true;
        }

        if(listainiz.equalsIgnoreCase("INPROGRESS") && listadest.equalsIgnoreCase("DONE")) {
            DONE.add(c);
            INPROGRESS.remove(c);
            c.update("DONE");
            return true;
        }

        if(listainiz.equalsIgnoreCase("TOBEREVISITED") && listadest.equalsIgnoreCase("DONE")) {
            DONE.add(c);
            TOBEREVISITED.remove(c);
            c.update("DONE");
            return true;
        }

        if(listainiz.equalsIgnoreCase("TOBEREVISITED") && listadest.equalsIgnoreCase("INPROGRESS")) {
            INPROGRESS.add(c);
            TOBEREVISITED.remove(c);
            c.update("INPROGRESS");
            return true;
        }
        return false;
    }


    @JsonIgnore
    public void setChatAddress(String address) {
        this.chatAddress = address;
    } //invocato solo dal managerDB quando i progetti vengono ripristinati

    @JsonIgnore
    public String getChatAddress() {
        return this.chatAddress;
    }

    @JsonIgnore
    public boolean canDelete() {
        if(TODO.isEmpty() && INPROGRESS.isEmpty() && TOBEREVISITED.isEmpty()) return true;
        else return false;
    }
}
