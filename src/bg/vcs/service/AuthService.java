package bg.vcs.service;

import bg.vcs.model.Role;
import bg.vcs.model.User;
import bg.vcs.repository.SqlRepository;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for user authentication and session management.
 */
public class AuthService {
    private Map<String, User> userDatabase = new HashMap<>();
    private User currentUser;
    private SqlRepository sqlRepository;

    public AuthService() {
        userDatabase.put("admin", new User("admin", Role.ADMIN));
        userDatabase.put("ivan", new User("ivan", Role.AUTHOR));
        userDatabase.put("maria", new User("maria", Role.REVIEWER));
        userDatabase.put("gosho", new User("gosho", Role.READER));
    }

    public void setSqlRepository(SqlRepository sqlRepository) {
        this.sqlRepository = sqlRepository;
    }

    public boolean login(String username) {
        String userLower = username.toLowerCase().trim();
        if (userDatabase.containsKey(userLower)) {
            this.currentUser = userDatabase.get(userLower);
            if (sqlRepository != null) {
                sqlRepository.setUserOnline(currentUser.getUsername());
            }
            System.out.println("\nSuccessfully logged in as " + currentUser.getUsername() + "!");
            return true;
        }
        System.out.println("\n[!] Error: User does not exist in the system.");
        return false;
    }

    public boolean createUserAndLogin(String username, Role role) {
        String userLower = username.toLowerCase().trim();
        if (userLower.isEmpty()) {
            System.out.println("\n[!] Error: Username cannot be empty.");
            return false;
        }
        
        User newUser = new User(userLower, role);
        userDatabase.put(userLower, newUser);
        this.currentUser = newUser;
        
        if (sqlRepository != null) {
            sqlRepository.ensureUserExists(currentUser.getUsername(), currentUser.getRole().toString());
            sqlRepository.setUserOnline(currentUser.getUsername());
        }
        
        System.out.println("\nSuccessfully created and logged in as " + currentUser.getUsername() + " [" + currentUser.getRole() + "]!");
        return true;
    }

    public void logout() {
        if (currentUser != null && sqlRepository != null) {
            sqlRepository.setUserOffline(currentUser.getUsername());
            System.out.println("\nUser " + currentUser.getUsername() + " logged out successfully.");
            currentUser = null;
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }
}