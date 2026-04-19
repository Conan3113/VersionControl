package bg.vcs.service;

import bg.vcs.exception.UnauthorizedException;
import bg.vcs.model.*;
import bg.vcs.repository.SqlRepository;
import java.util.Collection;

/**
 * Клас с автоматизирани тестове за DocumentService.
 * Може да се стартира независимо от main приложението.
 */
public class DocumentServiceTest {
    
    public static void runAllTests() {
        System.out.println("=== ЗАПОЧВАНЕ НА ТЕСТОВЕТЕ ===");
        
        testCreateDocument();
        testUpdateDocument();
        testReviewVersion();
        testUnauthorizedActions();
        testCompareVersions();
        
        System.out.println("=== КРАЙ НА ТЕСТОВЕТЕ ===");
    }
    
    private static void testCreateDocument() {
        System.out.println("\n--- Тест 1: Създаване на документ ---");
        
        AuthService authService = new AuthService();
        SqlRepository mockRepo = new SqlRepository(authService);
        AuditService auditService = new AuditService();
        DocumentService docService = new DocumentService(mockRepo, authService, auditService);
        
        // Логин като автор
        authService.login("ivan");
        
        try {
            docService.createDocument("TEST_DOC", "Test Document", "Initial content");
            System.out.println("✅ Документът е създаден успешно");
            
            Collection<Document> docs = docService.getAllDocuments();
            boolean found = docs.stream().anyMatch(d -> d.getId().equals("TEST_DOC"));
            System.out.println(found ? "✅ Документът е в списъка" : "❌ Документът липсва");
            
        } catch (Exception e) {
            System.out.println("❌ Грешка при създаване: " + e.getMessage());
        }
    }
    
    private static void testUpdateDocument() {
        System.out.println("\n--- Тест 2: Обновяване на документ ---");
        
        AuthService authService = new AuthService();
        SqlRepository mockRepo = new SqlRepository(authService);
        AuditService auditService = new AuditService();
        DocumentService docService = new DocumentService(mockRepo, authService, auditService);
        
        authService.login("ivan");
        
        try {
            // Първо създаваме документ
            docService.createDocument("UPDATE_TEST", "Update Test", "Version 1");
            
            // После го обновяваме
            docService.updateDocument("UPDATE_TEST", "Version 2 content");
            
            Collection<Document> docs = docService.getAllDocuments();
            Document doc = docs.stream().filter(d -> d.getId().equals("UPDATE_TEST")).findFirst().orElse(null);
            
            if (doc != null && doc.getVersions().size() == 2) {
                System.out.println("✅ Версия 2 е добавена успешно");
                System.out.println("   Брой версии: " + doc.getVersions().size());
            } else {
                System.out.println("❌ Проблем с добавянето на версия");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Грешка при обновяване: " + e.getMessage());
        }
    }
    
    private static void testReviewVersion() {
        System.out.println("\n--- Тест 3: Рецензиране на версия ---");
        
        AuthService authService = new AuthService();
        SqlRepository mockRepo = new SqlRepository(authService);
        AuditService auditService = new AuditService();
        DocumentService docService = new DocumentService(mockRepo, authService, auditService);
        
        try {
            // Автор създава документ
            authService.login("ivan");
            docService.createDocument("REVIEW_TEST", "Review Test", "Content for review");
            
            // Рецензент одобрява версията
            authService.login("maria");
            docService.reviewVersion("REVIEW_TEST", 1, Status.APPROVED, "Looks good!");
            
            Collection<Document> docs = docService.getAllDocuments();
            Document doc = docs.stream().filter(d -> d.getId().equals("REVIEW_TEST")).findFirst().orElse(null);
            
            if (doc != null && doc.getVersions().get(0).getStatus() == Status.APPROVED) {
                System.out.println("✅ Версията е одобрена успешно");
                System.out.println("   Статус: " + doc.getVersions().get(0).getStatus());
            } else {
                System.out.println("❌ Проблем с рецензирането");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Грешка при рецензиране: " + e.getMessage());
        }
    }
    
    private static void testUnauthorizedActions() {
        System.out.println("\n--- Тест 4: Неоторизирани действия ---");
        
        AuthService authService = new AuthService();
        SqlRepository mockRepo = new SqlRepository(authService);
        AuditService auditService = new AuditService();
        DocumentService docService = new DocumentService(mockRepo, authService, auditService);
        
        // Читател се опитва да създаде документ
        authService.login("gosho");
        
        try {
            docService.createDocument("UNAUTHORIZED_TEST", "Should Fail", "Content");
            System.out.println("❌ Читател успя да създаде документ (трябва да е невъзможно)");
        } catch (UnauthorizedException e) {
            System.out.println("✅ Читател не може да създава документи: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Неочаквана грешка: " + e.getMessage());
        }
    }
    
    private static void testCompareVersions() {
        System.out.println("\n--- Тест 5: Сравнение на версии ---");
        
        AuthService authService = new AuthService();
        SqlRepository mockRepo = new SqlRepository(authService);
        AuditService auditService = new AuditService();
        DocumentService docService = new DocumentService(mockRepo, authService, auditService);
        
        try {
            authService.login("ivan");
            
            // Създаваме документ с няколко версии
            docService.createDocument("COMPARE_TEST", "Compare Test", "First version content");
            docService.updateDocument("COMPARE_TEST", "Second version content");
            
            // Сравняваме версиите
            String diff = docService.compareVersions("COMPARE_TEST", 1, 2);
            
            if (diff.contains("Comparing Version 1 and 2")) {
                System.out.println("✅ Сравнението работи успешно");
                System.out.println("   Резултатът съдържа очаквания текст");
            } else {
                System.out.println("❌ Проблем със сравнението");
                System.out.println("   Резултат: " + diff);
            }
            
        } catch (Exception e) {
            System.out.println("❌ Грешка при сравнение: " + e.getMessage());
        }
    }
}
