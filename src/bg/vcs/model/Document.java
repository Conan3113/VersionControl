package bg.vcs.model;


import java.util.ArrayList;
import java.util.List;


public class Document{


    private String id;
    private String title;
    private List<Version> versions;

    public Document(String id, String title) {
        this.id = id;
        this.title = title;
        this.versions = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<Version> getVersions() { return versions; }

}