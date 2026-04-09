package bg.vcs.service;
import bg.vcs.model.Document;
import bg.vcs.exception.UnauthorizedException;
import bg.vcs.model.*;
import bg.vcs.repository.SqlRepository;
import java.util.*;

public class DocumentService {
    private Map<String, Document> documents;
    private SqlRepository sqlRepo;
    private AuthService authService;

    public DocumentService(SqlRepository sqlRepo, AuthService authService) {
        this.sqlRepo = sqlRepo;
        this.authService = authService;
        this.documents = sqlRepo.loadAll();
    }


    public void createDocument(String id, String title, String content) throws UnauthorizedException {
        User currentUser = authService.getCurrentUser();


        if (currentUser.getRole() == Role.READER) {
            throw new UnauthorizedException("Readers cannot create documents!");
        }

        Document doc = new Document(id, title);
        Version v = new Version(1, content, currentUser.getUsername());

        doc.getVersions().add(v);
        documents.put(id, doc);
        sqlRepo.syncDocument(doc);
    }


    public void updateDocument(String id, String newContent) {
        Document doc = documents.get(id);
        if (doc != null) {
            int nextVersionNum = doc.getVersions().size() + 1;
            Version newVersion = new Version(nextVersionNum, newContent, authService.getCurrentUser().getUsername());

            doc.getVersions().add(newVersion);
            sqlRepo.syncDocument(doc);
        }
    }


    public void reviewVersion(String docId, int vNum, Status status, String comment) {
        Document doc = documents.get(docId);
        if (doc != null && vNum <= doc.getVersions().size()) {
            Version v = doc.getVersions().get(vNum - 1);
            v.setStatus(status);
            v.setReviewerComment(comment);

            sqlRepo.syncDocument(doc);
        }
    }


    public String compareVersions(String docId, int vNum1, int vNum2) {
        Document doc = documents.get(docId);
        if (doc == null) return "Document not found.";


        if (vNum1 > doc.getVersions().size() || vNum2 > doc.getVersions().size()) {
            return "One or both versions do not exist.";
        }

        String text1 = doc.getVersions().get(vNum1 - 1).getContent();
        String text2 = doc.getVersions().get(vNum2 - 1).getContent();


        StringBuilder diff = new StringBuilder();
        diff.append("--- Comparing Version ").append(vNum1).append(" and ").append(vNum2).append(" ---\n");

        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");

        diff.append("Version ").append(vNum1).append(" words: ").append(Arrays.toString(words1)).append("\n");
        diff.append("Version ").append(vNum2).append(" words: ").append(Arrays.toString(words2)).append("\n");


        return diff.toString();
    }

    public Collection<Document> getAllDocuments() {
        return documents.values();
    }
}