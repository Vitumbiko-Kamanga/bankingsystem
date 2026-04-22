package bank.service;

import bank.model.Admin;

public class AdminService {
    private final Admin admin = new Admin();

    public boolean login(String username, String password) {
        return admin.login(username, password);
    }
}