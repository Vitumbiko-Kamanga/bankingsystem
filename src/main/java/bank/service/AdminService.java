package bank.service;

import bank.model.Admin;
import bank.model.User;
import java.util.Collection;

public class AdminService {
    private Admin admin;
    private UserService userService;
    private BankService bankService;

    public AdminService() {
        loadAdminPassword();
    }

    public AdminService(UserService userService, BankService bankService) {
        this();
        this.userService = userService;
        this.bankService = bankService;
    }

    private void loadAdminPassword() {
        try {
            String password = FileService.readAdminPassword();
            this.admin = new Admin(password);
        } catch (Exception e) {
            this.admin = new Admin();
        }
    }
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setBankService(BankService bankService) {
        this.bankService = bankService;
    }

    public boolean login(String username, String password) {
        return admin != null && admin.login(username, password);
    }

    public String changeAdminPassword(String oldPassword, String newPassword) {
        if (admin == null) return "ERROR: Admin not initialized.";

        if (!admin.login("admin", oldPassword)) {
            return "Current password incorrect.";
        }

        String trimmedNew = newPassword.trim();
        if (trimmedNew.isEmpty()) {
            return "New password cannot be empty.";
        }
        if (trimmedNew.length() < 6) {
            return "Password must be 6+ characters.";
        }
        if (trimmedNew.equals(oldPassword.trim())) {
            return "Must be different from current password.";
        }

        admin.changePassword(trimmedNew);
        FileService.saveAdminPassword(trimmedNew);
        return "Password changed & SAVED! Restart to use new password.";
    }

    public String deleteUser(String username) {
        if (userService == null || bankService == null) {
            return "ERROR: Services unavailable.";
        }

        String accountId = userService.getAccountIdByUsername(username);
        if (accountId == null) {
            return "User '" + username + "' not found.";
        }

        FileService.deleteUser(username);
        FileService.deleteAccount(accountId);
        userService.deleteUserFromCache(username);
        bankService.deleteAccountFromCache(accountId);

        return username + "' ('" + accountId + "') DELETED!";
    }

    public Collection<User> getAllUsers() {
        return userService != null ? userService.getAllUsers() : java.util.Collections.emptyList();
    }
}