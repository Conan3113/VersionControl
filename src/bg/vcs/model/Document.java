package bg.vcs.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Document implements Serializable {
    private static final long serialVersionUID = 1L;

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


    public Version getActiveVersion() {
        for (int i = versions.size() - 1; i >= 0; i--) {
            if (versions.get(i).getStatus() == Status.APPROVED) {
                return versions.get(i);
            }
        }
        return null;
    }
}