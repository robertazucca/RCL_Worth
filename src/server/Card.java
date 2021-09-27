package server;

//import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

public class Card implements Serializable {

    private String name;
    private String description;
    private List<String> history;

    public Card (String name, String description) {
        this.name = name;
        this.description = description;
        history = new ArrayList<>();
        history.add("TODO");
    }

    public Card(){
        this.name = null;
        this.description = null;
        this.history = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name;}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) { this.description = description;}

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> updates){ this.history = updates;}

    public void update(String newState) {
        this.history.add(newState);
    }

    @JsonIgnore
    public String getState() {
        StringTokenizer tokenizer = new StringTokenizer(this.history.get(history.size()-1));
        String n = tokenizer.nextToken();
        return n;
    }
}