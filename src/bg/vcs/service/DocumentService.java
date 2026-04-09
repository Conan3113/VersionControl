package bg.vcs.service;

import bg.vcs.exception.UnauthorizedException;
import bg.vcs.model.*;
import bg.vcs.repository.SqlRepository;
import java.util.*;

public class DocumentService {
    private Map<String, Document> documents;
    private SqlRepository sqlRepo;
    private AuthService authService;
    private AuditService auditService;

    public DocumentService(SqlRepository sqlRepo, AuthService authService, AuditService auditService) {
        this.sqlRepo = sqlRepo;
        this.authService = authService;
        this.auditService = auditService;
        this.documents = sqlRepo.loadAll(); // Зареждаме всичко от SQL при старт
    }

    // 1. СЪЗДАВАНЕ
    // Добави 'throws UnauthorizedException' в сигнатурата
    public void createDocument(String id, String title, String content) throws UnauthorizedException {
        User currentUser = authService.getCurrentUser();
        String username = currentUser.getUsername();

        // ПРОВЕРКА: Само AUTHOR и ADMIN могат да създават.
        if (currentUser.getRole() != Role.AUTHOR && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only Authors and Admins can create documents. Your role is: " + currentUser.getRole());
        }

        Document doc = new Document(id, title);
        Version v = new Version(1, content, username);

        doc.getVersions().add(v);
        documents.put(id, doc);
        sqlRepo.syncDocument(doc);
        auditService.log(username, "CREATE_DOCUMENT", "ID: " + id);
    }

    // 2. ОБНОВЯВАНЕ (Нова версия)
    public void updateDocument(String id, String newContent) throws UnauthorizedException {
        User currentUser = authService.getCurrentUser();
        // ПРОВЕРКА: Само AUTHOR и ADMIN могат да добавят нови версии.
        if (currentUser.getRole() != Role.AUTHOR && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only Authors and Admins can update documents. Your role is: " + currentUser.getRole());
        }
        Document doc = documents.get(id);
        if (doc != null) {
            int nextVersionNum = doc.getVersions().size() + 1;
            Version newVersion = new Version(nextVersionNum, newContent, currentUser.getUsername());

            doc.getVersions().add(newVersion);
            sqlRepo.syncDocument(doc);
            auditService.log(currentUser.getUsername(), "UPDATE_DOCUMENT", "ID: " + id + ", New Version: " + nextVersionNum);
        }
    }

    // 3. РЕЦЕНЗИРАНЕ
    public void reviewVersion(String docId, int vNum, Status status, String comment) throws UnauthorizedException {
        User currentUser = authService.getCurrentUser();
        // ПРОВЕРКА: Само REVIEWER и ADMIN могат да рецензират.
        if (currentUser.getRole() != Role.REVIEWER && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only Reviewers and Admins can review versions. Your role is: " + currentUser.getRole());
        }
        Document doc = documents.get(docId);
        if (doc != null && vNum <= doc.getVersions().size()) {
            Version v = doc.getVersions().get(vNum - 1);
            v.setStatus(status);
            v.setReviewerComment(comment);

            sqlRepo.syncDocument(doc);
            auditService.log(currentUser.getUsername(), "REVIEW_VERSION", "Doc: " + docId + ", V" + vNum + " -> " + status);
        }
    }

    // 4. СРАВНЕНИЕ НА ВЕРСИИ
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

    // 5. ВЗЕМАНЕ НА ВСИЧКИ ДОКУМЕНТИ
    public Collection<Document> getAllDocuments() {
        return documents.values();
    }
}