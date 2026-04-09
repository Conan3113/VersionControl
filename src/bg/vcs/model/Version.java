package bg.vcs.model;

public class Version {
    private int id;
    private String content;
    private String authorName;
    private Status status;
    private String reviewerComment;

    public Version(int id, String content, String authorName) {
        this.id = id;
        this.content = (content != null) ? content : "";
        this.authorName = (authorName != null) ? authorName : "Unknown";
        this.status = Status.PENDING;
        this.reviewerComment = "";
    }


    public int getId() { return id; }
    public String getContent() { return content; }
    public String getAuthorName() { return authorName; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getReviewerComment() { return reviewerComment; }
    public void setReviewerComment(String reviewerComment) { this.reviewerComment = reviewerComment; }
}