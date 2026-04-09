package bg.vcs.service;

import bg.vcs.model.Role;
import bg.vcs.model.User;
import java.util.HashMap;
import java.util.Map;


public class AuthService {
    private Map<String, User> userDatabase = new HashMap<>();
    private User currentUser;

    public AuthService() {
        userDatabase.put("admin", new User("admin", Role.ADMIN));
        userDatabase.put("ivan", new User("ivan", Role.AUTHOR));
        userDatabase.put("maria", new User("maria", Role.REVIEWER));
        userDatabase.put("gosho", new User("gosho", Role.READER));
    }

    public boolean login(String username) {
        String userLower = username.toLowerCase().trim();
        if (userDatabase.containsKey(userLower)) {
            this.currentUser = userDatabase.get(userLower);
            System.out.println("\nSuccessfully logged in as " + currentUser.getUsername() + "!");
            return true;
        }
        System.out.println("\n[!] Error: User does not exist in the system.");
        return false;
    }

    public User getCurrentUser() {
        return currentUser;
    }
}