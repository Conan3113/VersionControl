package bg.vcs;

import bg.vcs.repository.SqlRepository;
import bg.vcs.service.AuthService;
import bg.vcs.service.AuditService;
import bg.vcs.service.DocumentService;
import bg.vcs.service.DocumentServiceTest;
import bg.vcs.model.Document;
import bg.vcs.model.Status;
import bg.vcs.model.Version;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        SqlRepository sqlRepo = new SqlRepository();
        AuthService authService = new AuthService();
        AuditService auditService = new AuditService();


        DocumentService documentService = new DocumentService(sqlRepo, authService, auditService);

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Добре дошли в VCS System 2026 ===");


        System.out.print("Въведете потребителско име (ivan, admin, maria, gosho): ");
        String username = scanner.nextLine();
        authService.login(username);

        if (authService.getCurrentUser() == null) {
            System.out.println("Грешен потребител! Програмата спира.");
            return;
        }

        System.out.println("Здравей, " + authService.getCurrentUser().getUsername() +
                " [" + authService.getCurrentUser().getRole() + "]");


        boolean running = true;
        while (running) {
            System.out.println("\n--- ГЛАВНО МЕНЮ ---");
            System.out.println("1. Списък на всички документи");
            System.out.println("2. Създаване на нов документ");
            System.out.println("3. Добавяне на нова версия");
            System.out.println("4. Преглед на документ (история)");
            System.out.println("5. Сравнение на версии (Diff)");
            System.out.println("6. Рецензиране на версия");
            System.out.println("7. Изпълнение на тестове");
            System.out.println("0. Изход");
            System.out.print("Избор: ");

            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1":
                        System.out.println("\nСписък документи в базата:");
                        documentService.getAllDocuments().forEach(d ->
                                System.out.println("- ID: " + d.getId() + " | Title: " + d.getTitle() + " (" + d.getVersions().size() + " версии)"));
                        break;

                    case "2":
                        System.out.print("Въведете ID на документа: ");
                        String id = scanner.nextLine();
                        System.out.print("Въведете заглавие: ");
                        String title = scanner.nextLine();
                        System.out.print("Въведете начално съдържание: ");
                        String content = scanner.nextLine();

                        documentService.createDocument(id, title, content);
                        break;

                    case "3":
                        System.out.print("Въведете ID на документа за обновяване: ");
                        String updateId = scanner.nextLine();
                        System.out.print("Въведете новото съдържание: ");
                        String newContent = scanner.nextLine();

                        documentService.updateDocument(updateId, newContent);
                        break;

                    case "4":
                        System.out.print("Въведете ID на документа: ");
                        String viewId = scanner.nextLine();
                        Document doc = documentService.getAllDocuments().stream()
                                .filter(d -> d.getId().equals(viewId)).findFirst().orElse(null);

                        if (doc != null) {
                            System.out.println("История на " + doc.getTitle() + ":");
                            for (Version v : doc.getVersions()) {
                                System.out.println("  V" + v.getId() + " | Автор: " + v.getAuthorName() + " | Статус: " + v.getStatus());
                                System.out.println("  Съдържание: " + v.getContent());
                                System.out.println("  -------------------");
                            }
                        } else {
                            System.out.println("Документът не е намерен.");
                        }
                        break;

                    case "5":
                        System.out.print("ID на документ: ");
                        String dId = scanner.nextLine();
                        System.out.print("Версия 1: ");
                        int v1 = Integer.parseInt(scanner.nextLine());
                        System.out.print("Версия 2: ");
                        int v2 = Integer.parseInt(scanner.nextLine());

                        System.out.println(documentService.compareVersions(dId, v1, v2));
                        break;

                    case "6":
                        System.out.print("ID на документ за рецензия: ");
                        String reviewDocId = scanner.nextLine();
                        System.out.print("Номер на версия за рецензия: ");
                        int reviewVNum = Integer.parseInt(scanner.nextLine());
                        System.out.print("Статус (APPROVED/REJECTED): ");
                        Status status = Status.valueOf(scanner.nextLine().toUpperCase());
                        System.out.print("Коментар (опционално): ");
                        String comment = scanner.nextLine();

                        documentService.reviewVersion(reviewDocId, reviewVNum, status, comment);
                        System.out.println("Версията е рецензирана успешно!");
                        break;

                    case "7":
                        System.out.println("\n=== ИЗПЪЛНЕНИЕ НА ТЕСТОВЕТЕ ===");
                        DocumentServiceTest.runAllTests();
                        break;

                    case "0":
                        running = false;
                        System.out.println("Излизане...");
                        auditService.printLogs();
                        break;

                    default:
                        System.out.println("Невалиден избор.");
                }
            } catch (Exception e) {
                System.err.println("ГРЕШКА: " + e.getMessage());
            }
        }
        scanner.close();
    }
}