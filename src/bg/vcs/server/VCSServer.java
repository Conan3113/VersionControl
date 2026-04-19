package bg.vcs.server;

import bg.vcs.model.*;
import bg.vcs.repository.SqlRepository;
import bg.vcs.service.AuthService;
import bg.vcs.service.AuditService;
import bg.vcs.service.DocumentService;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * VCS Server - Handles multiple client connections and manages shared database
 */
public class VCSServer {
    private static final int PORT = 8080;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    private SqlRepository sharedSqlRepository;

    public VCSServer() {
        this.executor = Executors.newCachedThreadPool();
        this.sharedSqlRepository = new SqlRepository(new AuthService());
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("=== VCS SERVER STARTED ON PORT " + PORT + " ===");
            System.out.println("Waiting for client connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, sharedSqlRepository);
                executor.submit(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientId;
        private User currentUser;
        private AuthService authService;
        private SqlRepository sqlRepository;
        private DocumentService documentService;
        private AuditService auditService;

        public ClientHandler(Socket socket, SqlRepository sharedRepository) {
            this.socket = socket;
            // Create separate services for each client but use shared repository
            this.authService = new AuthService();
            this.sqlRepository = sharedRepository;
            this.authService.setSqlRepository(sqlRepository);
            this.auditService = new AuditService();
            this.documentService = new DocumentService(sqlRepository, authService, auditService);
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                clientId = socket.getInetAddress().toString() + ":" + socket.getPort();
                System.out.println("New client connected: " + clientId);

                // Send welcome message
                sendMessage("WELCOME", "VCS Server v1.0. Connected successfully!");

                // Process client requests
                String request;
                while ((request = in.readLine()) != null) {
                    processRequest(request);
                }
            } catch (IOException e) {
                System.err.println("Client handler error: " + e.getMessage());
            } finally {
                disconnectClient();
            }
        }

        private void processRequest(String request) {
            try {
                String[] parts = request.split("\\|", 3);
                String command = parts[0];
                String data = parts.length > 1 ? parts[1] : "";
                String params = parts.length > 2 ? parts[2] : "";

                switch (command) {
                    case "LOGIN":
                        handleLogin(data);
                        break;
                    case "CREATE_USER":
                        handleCreateUser(data, params);
                        break;
                    case "GET_DOCUMENTS":
                        handleGetDocuments();
                        break;
                    case "CREATE_DOCUMENT":
                        handleCreateDocument(data, params);
                        break;
                    case "UPDATE_DOCUMENT":
                        handleUpdateDocument(data, params);
                        break;
                    case "GET_DOCUMENT":
                        handleGetDocument(data);
                        break;
                    case "COMPARE_VERSIONS":
                        handleCompareVersions(data, params);
                        break;
                    case "REVIEW_VERSION":
                        handleReviewVersion(data, params);
                        break;
                    case "LOGOUT":
                        handleLogout();
                        break;
                    default:
                        sendMessage("ERROR", "Unknown command: " + command);
                }
            } catch (Exception e) {
                sendMessage("ERROR", "Processing error: " + e.getMessage());
            }
        }

        private void handleLogin(String username) {
            if (authService.login(username)) {
                currentUser = authService.getCurrentUser();
                connectedClients.put(username, this);
                sendMessage("LOGIN_SUCCESS", currentUser.getUsername() + "|" + currentUser.getRole());
                System.out.println("User logged in: " + username);
            } else {
                sendMessage("LOGIN_FAILED", "Invalid username");
            }
        }

        private void handleCreateUser(String username, String roleStr) {
            try {
                Role role = Role.valueOf(roleStr);
                if (authService.createUserAndLogin(username, role)) {
                    User newUser = authService.getCurrentUser();
                    currentUser = newUser;
                    connectedClients.put(username, this);
                    sendMessage("CREATE_USER_SUCCESS", currentUser.getUsername() + "|" + currentUser.getRole());
                    System.out.println("New user created and logged in: " + username);
                } else {
                    sendMessage("CREATE_USER_FAILED", "Failed to create user");
                }
            } catch (Exception e) {
                sendMessage("CREATE_USER_FAILED", "Invalid role: " + roleStr);
            }
        }

        private void handleGetDocuments() {
            try {
                Collection<Document> documents = documentService.getAllDocuments();
                StringBuilder response = new StringBuilder();
                for (Document doc : documents) {
                    response.append(doc.getId()).append("|")
                           .append(doc.getTitle()).append("|")
                           .append(doc.getVersions().size()).append(";");
                }
                sendMessage("DOCUMENTS_LIST", response.toString());
            } catch (Exception e) {
                sendMessage("ERROR", "Failed to get documents: " + e.getMessage());
            }
        }

        private void handleCreateDocument(String id, String titleAndContent) {
            try {
                String[] parts = titleAndContent.split("\\|", 2);
                String title = parts[0];
                String content = parts.length > 1 ? parts[1] : "";
                
                documentService.createDocument(id, title, content);
                sendMessage("CREATE_DOCUMENT_SUCCESS", "Document created successfully");
                System.out.println("Document created by " + currentUser.getUsername() + ": " + id);
            } catch (Exception e) {
                sendMessage("ERROR", "Failed to create document: " + e.getMessage());
            }
        }

        private void handleUpdateDocument(String id, String content) {
            try {
                documentService.updateDocument(id, content);
                sendMessage("UPDATE_DOCUMENT_SUCCESS", "Document updated successfully");
                System.out.println("Document updated by " + currentUser.getUsername() + ": " + id);
            } catch (Exception e) {
                sendMessage("ERROR", "Failed to update document: " + e.getMessage());
            }
        }

        private void handleGetDocument(String id) {
            try {
                documentService.refreshDocuments(); // Refresh to get latest data
                Document doc = documentService.getAllDocuments().stream()
                    .filter(d -> d.getId().equals(id))
                    .findFirst()
                    .orElse(null);
                
                if (doc != null) {
                    StringBuilder response = new StringBuilder();
                    response.append(doc.getId()).append("|")
                           .append(doc.getTitle()).append(";");
                    
                    for (Version v : doc.getVersions()) {
                        response.append(v.getId()).append("|")
                               .append(v.getContent()).append("|")
                               .append(v.getAuthorName()).append("|")
                               .append(v.getStatus()).append("|")
                               .append(v.getReviewerComment() != null ? v.getReviewerComment() : "").append(";");
                    }
                    
                    sendMessage("DOCUMENT_DETAILS", response.toString());
                } else {
                    sendMessage("ERROR", "Document not found: " + id);
                }
            } catch (Exception e) {
                sendMessage("ERROR", "Failed to get document: " + e.getMessage());
            }
        }

        private void handleCompareVersions(String docId, String versions) {
            try {
                String[] vParts = versions.split("\\|");
                String v1 = vParts[0];
                String v2 = vParts[1];
                String diff = documentService.compareVersions(docId, Integer.parseInt(v1), Integer.parseInt(v2));
                sendMessage("COMPARE_RESULT", diff);
            } catch (Exception e) {
                sendMessage("ERROR", "Failed to compare versions: " + e.getMessage());
            }
        }

        private void handleReviewVersion(String docId, String versionData) {
            try {
                String[] parts = versionData.split("\\|", 3);
                String vNum = parts[0];
                String status = parts[1];
                String comment = parts.length > 2 ? parts[2] : "";
                
                Status docStatus = Status.valueOf(status);
                documentService.reviewVersion(docId, Integer.parseInt(vNum), docStatus, comment);
                sendMessage("REVIEW_SUCCESS", "Version reviewed successfully");
                System.out.println("Document reviewed by " + currentUser.getUsername() + ": " + docId + " v" + vNum);
            } catch (Exception e) {
                sendMessage("ERROR", "Failed to review version: " + e.getMessage());
            }
        }

        private void handleLogout() {
            if (currentUser != null) {
                authService.logout();
                connectedClients.remove(currentUser.getUsername());
                System.out.println("User logged out: " + currentUser.getUsername());
                currentUser = null;
            }
            sendMessage("LOGOUT_SUCCESS", "Logged out successfully");
        }

        private void sendMessage(String type, String message) {
            out.println(type + "|" + message);
        }

        private void disconnectClient() {
            try {
                if (currentUser != null) {
                    authService.logout();
                    connectedClients.remove(currentUser.getUsername());
                    System.out.println("User disconnected: " + currentUser.getUsername());
                }
                
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                
                System.out.println("Client disconnected: " + clientId);
            } catch (IOException e) {
                System.err.println("Error disconnecting client: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        VCSServer server = new VCSServer();
        server.start();
    }
}
