package server;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public interface Project {

    String getProjectName();

    String getCreator();

    List<String> getMembers();

    void addProjectMember ( String member);


    void addCards(List<Card> cards);

    List<Card> getCards();

    Card getCard(String name);

    void createCard(String name, String description);

    boolean moveeCard(String cardName, String listaPartenza, String listDestinazione);

    //void setChatAddress(String address);

     //String getChatAddress();

     //boolean canDelete();
}
