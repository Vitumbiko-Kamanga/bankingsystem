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
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    // LOAD USERS FROM FILE
    public void loadUsers() {
        for (String line : FileService.readUsers()) {
            String[] p = line.split(",");
            users.put(p[0], new User(p[0], p[1], p[2]));
        }
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }
}