package bank.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Transaction {
    public enum Type {
        DEPOSIT, WITHDRAWAL, TRANSFER
    }

    private final Type type;
    private final double amount;
    private final LocalDateTime timestamp;
    private final String description;
    private String accountId;

    public Transaction(Type type, double amount, String description) {
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public Transaction(Type type, double amount, String description, LocalDateTime timestamp) {
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
    }

    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getAccountId() { return accountId != null ? accountId : "Unknown"; }
    public Type getType() { return type; }
    public Double getAmount() { return amount; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getFormattedDate() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Override
    public String toString() {
        String emoji = switch (type) {
            case DEPOSIT -> "💰"; case WITHDRAWAL -> "💸"; case TRANSFER -> "🔄";
        };
        return String.format("%s %s | MWK %.2f | %s | %s (%s)",
                emoji, type, amount, getFormattedTimestamp(), description, getAccountId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Transaction)) return false;
        Transaction t = (Transaction) obj;
        return Double.compare(t.amount, amount) == 0 &&
                type == t.type &&
                timestamp.equals(t.timestamp) &&
                Objects.equals(description, t.description) &&
                Objects.equals(accountId, t.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, amount, timestamp, description, accountId);
    }
}