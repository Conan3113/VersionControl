package bg.vcs.service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервиз за записване на одит лог (история на действията).
 */
public class AuditService {
    private List<String> logs = new ArrayList<>();
    private static final String AUDIT_FILE = "system_audit.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void log(String username, String action, String details) {
        String entry = String.format("[%s] ПОТРЕБИТЕЛ: %s | ДЕЙСТВИЕ: %s | ОБЕКТ: %s",
                LocalDateTime.now().format(FORMATTER), username, action, details);
        
        logs.add(entry);
        
        // Запис във външен текстов файл
        try (PrintWriter out = new PrintWriter(new FileWriter(AUDIT_FILE, true))) {
            out.println(entry);
        } catch (IOException e) {
            System.err.println("Warning: Failed to write to audit file.");
        }
    }

    public void printLogs() {
        System.out.println("\n---SYSTEM AUDIT LOGS---");
        if (logs.isEmpty()) {
            System.out.println("No registered users in current session.");
        } else {
            logs.forEach(System.out::println);
        }
        System.out.println("=========================================");
    }
}
