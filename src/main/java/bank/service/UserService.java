package bank.service;

import bank.model.User;
import java.util.*;

public class UserService {

    private final Map<String, User> users = new HashMap<>();

    public void register(User user) {
        users.put(user.getUsername(), user);
        FileService.saveUser(user.getUsername(), user.getPassword(), user.getAccountId());
    }

    public User login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.getPassword().equals(password)) return user;
        return null;
    }

    public User authenticate(String username, String password) {
        return login(username, password);
    }

    public void loadUsers() {
        for (String line : FileService.readUsers()) {
            String[] p = line.split(",");
            if (p.length == 3) {  // Safety check
                users.put(p[0], new User(p[0], p[1], p[2]));
            }
        }
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }

    // DELETE SUPPORT
    public String getAccountIdByUsername(String username) {
        User user = users.get(username);
        return user != null ? user.getAccountId() : null;
    }

    public void deleteUserFromCache(String username) {
        users.remove(username);
    }


    // ───────── CHANGE PASSWORD ──────
    public String changePassword(String username, String oldPassword, String newPassword) {
        User user = users.get(username);

        if (user == null)
            return "ERROR: User not found.";

        if (!user.getPassword().equals(oldPassword))
            return "ERROR: Current password is incorrect.";

        if (newPassword == null || newPassword.trim().isEmpty())
            return "ERROR: New password cannot be empty.";

        if (newPassword.equals(oldPassword))
            return "ERROR: New password must be different from the current one.";

        users.put(username, new User(username, newPassword, user.getAccountId()));

        boolean saved = FileService.updateUserPassword(username, newPassword);
        return saved ? "Password changed successfully." : "ERROR: Could not save new password.";
    }
}