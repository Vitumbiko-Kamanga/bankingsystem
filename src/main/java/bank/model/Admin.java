package bank.model;

public class Admin {
    private final String username = "admin";
    private final String password = "admin123";

    public boolean login(String user, String pass) {
        return username.equals(user) && password.equals(pass);
    }
}