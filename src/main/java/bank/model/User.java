package bank.model;

public class User {
    private final String username;
    private final String password;
    private final String accountId;

    public User(String username, String password, String accountId) {
        this.username = username;
        this.password = password;
        this.accountId = accountId;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getAccountId() { return accountId; }
}