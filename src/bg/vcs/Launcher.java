package bg.vcs;

import java.util.Scanner;

/**
 * Launcher for VCS System - allows starting server or client
 */
public class Launcher {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== VCS System Launcher ===");
        System.out.println("1. Start VCS Server");
        System.out.println("2. Start VCS Client");
        System.out.print("Choose option (1-2): ");
        
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1":
                System.out.println("Starting VCS Server...");
                bg.vcs.server.VCSServer.main(args);
                break;
            case "2":
                System.out.println("Starting VCS Client...");
                bg.vcs.client.VCSClient.main(args);
                break;
            default:
                System.out.println("Invalid choice. Exiting.");
        }
        
        scanner.close();
    }
}
