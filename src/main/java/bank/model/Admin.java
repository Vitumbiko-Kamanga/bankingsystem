package bank.model;

public class Admin {
    private final String username = "admin";
    private String password;

    public Admin(String password) {
        this.password = password != null ? password : "admin123";
    }

    public Admin() {
        this("admin123");
    }

    public boolean login(String user, String pass) {
        return username.equals(user) && password.equals(pass);
    }

    public void changePassword(String newPassword) {
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            this.password = newPassword;
        }
    }

    public String getPassword() {
        return password;
    }
}