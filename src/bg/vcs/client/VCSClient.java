package bg.vcs.client;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * VCS Client - Connects to VCS Server and handles user interaction
 */
public class VCSClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;
    private boolean connected = false;
    private String currentUser = "";
    private String currentRole = "";

    public void start() {
        scanner = new Scanner(System.in);
        
        try {
            connectToServer();
            runClientLoop();
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void connectToServer() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
        
        // Read welcome message
        String welcome = readResponse();
        System.out.println(welcome);
    }

    private void runClientLoop() {
        System.out.println("=== VCS CLIENT 2026 ===");
        
        // User creation/login
        if (!handleUserLogin()) {
            return;
        }

        // Main menu loop
        boolean running = true;
        while (running && connected) {
            showMainMenu();
            String choice = scanner.nextLine();
            
            try {
                switch (choice) {
                    case "1":
                        handleGetDocuments();
                        break;
                    case "2":
                        handleCreateDocument();
                        break;
                    case "3":
                        handleUpdateDocument();
                        break;
                    case "4":
                        handleGetDocument();
                        break;
                    case "5":
                        handleCompareVersions();
                        break;
                    case "6":
                        handleReviewVersion();
                        break;
                    case "0":
                        running = false;
                        handleLogout();
                        break;
                    default:
                        System.out.println("Невалиден избор.");
                }
            } catch (Exception e) {
                System.err.println("Грешка: " + e.getMessage());
            }
        }
    }

    private boolean handleUserLogin() {
        System.out.println("\n--- СЪЗДАВАНЕ НА ПОТРЕБИТЕЛ ---");
        System.out.print("Въведете потребителско име: ");
        String username = scanner.nextLine();
        
        System.out.println("\nИзберете роля:");
        System.out.println("1. AUTHOR - Може да създава и обновява документи");
        System.out.println("2. REVIEWER - Може да рецензира документи");
        System.out.println("3. READER - Може да преглежда документи");
        System.out.println("4. ADMIN - Пълен достъп");
        System.out.print("Избор (1-4): ");
        
        String roleStr = "READER";
        try {
            int roleChoice = Integer.parseInt(scanner.nextLine());
            switch (roleChoice) {
                case 1: roleStr = "AUTHOR"; break;
                case 2: roleStr = "REVIEWER"; break;
                case 3: roleStr = "READER"; break;
                case 4: roleStr = "ADMIN"; break;
                default: System.out.println("Невалиден избор. Избрана е роля READER."); break;
            }
        } catch (NumberFormatException e) {
            System.out.println("Невалиден избор. Избрана е роля READER.");
        }
        
        // Send create user request
        sendRequest("CREATE_USER", username, roleStr);
        String response = readResponse();
        
        if (response.startsWith("CREATE_USER_SUCCESS")) {
            String[] parts = response.split("\\|", 2);
            currentUser = parts[1];
            currentRole = parts.length > 2 ? parts[2] : "";
            System.out.println("Здравей, " + currentUser + " [" + currentRole + "]");
            return true;
        } else {
            System.out.println("Грешка при създаване на потребител!");
            return false;
        }
    }

    private void showMainMenu() {
        System.out.println("\n--- ГЛАВНО МЕНЮ ---");
        System.out.println("1. Списък на всички документи");
        System.out.println("2. Създаване на нов документ");
        System.out.println("3. Добавяне на нова версия");
        System.out.println("4. Преглед на документ (история)");
        System.out.println("5. Сравнение на версии (Diff)");
        System.out.println("6. Рецензиране на версия");
        System.out.println("0. Изход");
        System.out.print("Избор: ");
    }

    private void handleGetDocuments() {
        sendRequest("GET_DOCUMENTS", "", "");
        String response = readResponse();
        
        if (response.startsWith("DOCUMENTS_LIST")) {
            String data = response.substring(response.indexOf('|') + 1);
            if (data.isEmpty()) {
                System.out.println("Няма документи в базата.");
            } else {
                System.out.println("\nСписък документи в базата:");
                String[] docs = data.split(";");
                for (String doc : docs) {
                    if (!doc.isEmpty()) {
                        String[] parts = doc.split("\\|");
                        System.out.println("- ID: " + parts[0] + " | Title: " + parts[1] + " (" + parts[2] + " версии)");
                    }
                }
            }
        } else {
            System.out.println("Грешка: " + response);
        }
    }

    private void handleCreateDocument() {
        System.out.print("Въведете ID на документа: ");
        String id = scanner.nextLine();
        System.out.print("Въведете заглавие: ");
        String title = scanner.nextLine();
        System.out.print("Въведете начално съдържание: ");
        String content = scanner.nextLine();
        
        sendRequest("CREATE_DOCUMENT", id, title + "|" + content);
        String response = readResponse();
        
        if (response.startsWith("CREATE_DOCUMENT_SUCCESS")) {
            System.out.println("Документът е създаден успешно!");
        } else {
            System.out.println("Грешка: " + response);
        }
    }

    private void handleUpdateDocument() {
        System.out.print("Въведете ID на документа за обновяване: ");
        String id = scanner.nextLine();
        System.out.print("Въведете новото съдържание: ");
        String content = scanner.nextLine();
        
        sendRequest("UPDATE_DOCUMENT", id, content);
        String response = readResponse();
        
        if (response.startsWith("UPDATE_DOCUMENT_SUCCESS")) {
            System.out.println("Документът е обновен успешно!");
        } else {
            System.out.println("Грешка: " + response);
        }
    }

    private void handleGetDocument() {
        System.out.print("Въведете ID на документа: ");
        String id = scanner.nextLine();
        
        sendRequest("GET_DOCUMENT", id, "");
        String response = readResponse();
        
        if (response.startsWith("DOCUMENT_DETAILS")) {
            String data = response.substring(response.indexOf('|') + 1);
            String[] parts = data.split(";");
            
            if (parts.length > 0) {
                String[] docInfo = parts[0].split("\\|");
                System.out.println("\nHistory of " + docInfo[1] + ":");
                
                for (int i = 1; i < parts.length; i++) {
                    if (!parts[i].isEmpty()) {
                        String[] versionParts = parts[i].split("\\|");
                        System.out.println("  V" + versionParts[0] + " | Автор: " + versionParts[2] + " | Статус: " + versionParts[3]);
                        System.out.println("  Съдържание: " + versionParts[1]);
                        if (versionParts.length > 4 && !versionParts[4].isEmpty()) {
                            System.out.println("  Коментар: " + versionParts[4]);
                        }
                        System.out.println("  -------------------");
                    }
                }
            }
        } else {
            System.out.println("Грешка: " + response);
        }
    }

    private void handleCompareVersions() {
        System.out.print("ID на документ: ");
        String dId = scanner.nextLine();
        System.out.print("Версия 1: ");
        String v1 = scanner.nextLine();
        System.out.print("Версия 2: ");
        String v2 = scanner.nextLine();
        
        sendRequest("COMPARE_VERSIONS", dId, v1 + "|" + v2);
        String response = readResponse();
        
        if (response.startsWith("COMPARE_RESULT")) {
            String diff = response.substring(response.indexOf('|') + 1);
            System.out.println(diff);
        } else {
            System.out.println("Грешка: " + response);
        }
    }

    private void handleReviewVersion() {
        System.out.print("ID на документ за рецензия: ");
        String reviewDocId = scanner.nextLine();
        System.out.print("Номер на версия за рецензия: ");
        String reviewVNum = scanner.nextLine();
        System.out.print("Статус (APPROVED/REJECTED): ");
        String status = scanner.nextLine();
        System.out.print("Comment (optional): ");
        String comment = scanner.nextLine();
        
        sendRequest("REVIEW_VERSION", reviewDocId, reviewVNum + "|" + status + "|" + comment);
        String response = readResponse();
        
        if (response.startsWith("REVIEW_SUCCESS")) {
            System.out.println("Версията е рецензирана успешно!");
        } else {
            System.out.println("Грешка: " + response);
        }
    }

    private void handleLogout() {
        sendRequest("LOGOUT", "", "");
        String response = readResponse();
        System.out.println("Излизане...");
    }

    private void sendRequest(String command, String data, String params) {
        out.println(command + "|" + data + "|" + params);
    }

    private String readResponse() {
        try {
            return in.readLine();
        } catch (IOException e) {
            connected = false;
            return "ERROR|Connection lost";
        }
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (scanner != null) {
                scanner.close();
            }
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        VCSClient client = new VCSClient();
        client.start();
    }
}
