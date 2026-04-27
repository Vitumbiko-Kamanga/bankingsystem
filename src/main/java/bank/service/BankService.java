package bank.service;

import bank.exception.FraudDetectedException;
import bank.model.Account;
import bank.model.SavingsAccount;
import bank.model.Transaction;

import java.util.*;
import java.util.concurrent.*;

public class BankService {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final FraudDetectionService fraudService = new FraudDetectionService(); //  ADDED

    public void addAccount(Account acc) {
        accounts.put(acc.getAccountId(), acc);
        FileService.saveAccount(acc.getAccountId(), acc.getAccountNumber(), acc.getOwner());
    }

    public void loadAccounts() {
        for (String line : FileService.readAccounts()) {
            String[] p = line.split(",");
            if (p.length >= 3) {
                accounts.put(p[0], new SavingsAccount(p[0], p[1], p[2], 0));
            }
        }
    }

    public Account getAccount(String id) {
        return accounts.get(id);
    }

    public Account getByAccountNumber(String accNo) {
        return accounts.values().stream()
                .filter(a -> a.getAccountNumber().equals(accNo))
                .findFirst().orElse(null);
    }

    public void deleteAccountFromCache(String accountId) {
        accounts.remove(accountId);
    }

    // ─────────────────────────────────────────────────────────────
    // DEPOSIT with FRAUD CHECK
    // ─────────────────────────────────────────────────────────────
    public String deposit(String id, double amount) {
        Account acc = accounts.get(id);
        if (acc == null) return " Account not found";
        if (amount <= 0) return " Amount must be positive";

        //  FRAUD DETECTION
        try {
            fraudService.check(acc, amount);
        } catch (FraudDetectedException e) {
            return " FRAUD ALERT: " + e.getMessage();
        }

        acc.deposit(amount);
        FileService.saveTransaction(id, "DEPOSIT", amount, "Deposit");
        return "✅ Deposit successful: MWK " + String.format("%.2f", amount);
    }

    // ─────────────────────────────────────────────────────────────
    // WITHDRAW with FRAUD CHECK
    // ─────────────────────────────────────────────────────────────
    public String withdraw(String id, double amount) {
        Account acc = accounts.get(id);
        if (acc == null) return " Account not found";
        if (amount <= 0) return " Amount must be positive";

        //  FRAUD DETECTION
        try {
            fraudService.check(acc, amount);
        } catch (FraudDetectedException e) {
            return " FRAUD ALERT: " + e.getMessage();
        }

        try {
            double currentBalance = calculateBalance(id);
            if (amount > currentBalance) {
                return " Insufficient funds (Balance: MWK " + String.format("%.2f", currentBalance) + ")";
            }
            acc.withdraw(amount);
            FileService.saveTransaction(id, "WITHDRAWAL", amount, "Withdrawal");
            return " Withdrawal successful: MWK " + String.format("%.2f", amount);
        } catch (Exception e) {
            return " " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // TRANSFER with FRAUD CHECK (on sender)
    // ─────────────────────────────────────────────────────────────
    public String transfer(String fromId, String toAccNo, double amount) {
        Account from = accounts.get(fromId);
        Account to = getByAccountNumber(toAccNo);

        if (from == null) return " Sender account not found";
        if (to == null)   return " Recipient '" + toAccNo + "' not found";
        if (from.getAccountNumber().equals(toAccNo)) return " Cannot transfer to same account";
        if (amount <= 0) return " Amount must be positive";

        //  FRAUD DETECTION on sender
        try {
            fraudService.check(from, amount);
        } catch (FraudDetectedException e) {
            return " FRAUD ALERT: " + e.getMessage();
        }

        try {
            double fromBalance = calculateBalance(fromId);
            if (amount > fromBalance) {
                return " Insufficient funds (Balance: MWK " + String.format("%.2f", fromBalance) + ")";
            }

            from.withdraw(amount);
            to.deposit(amount);

            FileService.saveTransaction(from.getAccountId(), "TRANSFER", amount, "Sent to " + toAccNo);
            FileService.saveTransaction(to.getAccountId(),   "TRANSFER", amount, "Received from " + fromId);

            return " Transfer successful: MWK " + String.format("%.2f", amount) +
                    " → " + to.getOwner();
        } catch (Exception e) {
            return " " + e.getMessage();
        }
    }

    public String transferByAccountNumber(String accountId, String toAccountNumber, double amount) {
        return transfer(accountId, toAccountNumber, amount);
    }

    // ─────────────────────────────────────────────────────────────
    // BALANCE CALCULATION (handles transfers)
    // ─────────────────────────────────────────────────────────────
    public double calculateBalance(String accId) {
        return FileService.getAllTransactionsAsObjects().stream()
                .filter(t -> t.getAccountId().equals(accId))
                .mapToDouble(t -> {
                    switch (t.getType()) {
                        case DEPOSIT:
                            return +t.getAmount();
                        case WITHDRAWAL:
                            return -t.getAmount();
                        case TRANSFER:
                            if (t.getDescription().contains("Sent to")) {
                                return -t.getAmount();
                            } else if (t.getDescription().contains("Received from")) {
                                return +t.getAmount();
                            }
                            return 0.0;
                        default:
                            return 0.0;
                    }
                })
                .sum();
    }

    public String getBalanceDisplay(String accId) {
        double balance = calculateBalance(accId);
        return String.format("💰 Balance: MWK %.2f", balance);
    }

    public Collection<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public double getTotalBankBalance() {
        return accounts.values().stream()
                .mapToDouble(acc -> calculateBalance(acc.getAccountId()))
                .sum();
    }

    public Map<Transaction.Type, Long> getTransactionStats() {
        return FileService.getAllTransactionsAsObjects().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Transaction::getType,
                        java.util.stream.Collectors.counting()
                ));
    }

    public void shutdown() {
        executor.shutdown();
    }
}