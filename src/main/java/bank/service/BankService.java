package bank.service;

import bank.model.*;

import java.util.*;
import java.util.concurrent.*;

public class BankService {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    public void addAccount(Account acc) {
        accounts.put(acc.getAccountId(), acc);
        FileService.saveAccount(acc.getAccountId(), acc.getAccountNumber(), acc.getOwner());
    }

    public void loadAccounts() {
        for (String line : FileService.readAccounts()) {
            String[] p = line.split(",");
            accounts.put(p[0], new SavingsAccount(p[0], p[1], p[2], 0));
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

    // ───────── TRANSACTIONS ─────────
    public Future<String> deposit(String id, double amount) {
        return executor.submit(() -> {
            Account acc = accounts.get(id);
            acc.deposit(amount);

            FileService.saveTransaction(id, "DEPOSIT", amount, "Deposit");

            return "Deposit successful";
        });
    }

    public Future<String> withdraw(String id, double amount) {
        return executor.submit(() -> {
            Account acc = accounts.get(id);
            acc.withdraw(amount);

            FileService.saveTransaction(id, "WITHDRAWAL", amount, "Withdraw");

            return "Withdraw successful";
        });
    }

    public Future<String> transfer(String fromId, String toAccNo, double amount) {
        return executor.submit(() -> {

            Account from = accounts.get(fromId);
            Account to = getByAccountNumber(toAccNo);

            from.withdraw(amount);
            to.deposit(amount);

            FileService.saveTransaction(fromId, "TRANSFER", amount, "Sent to " + toAccNo);
            FileService.saveTransaction(to.getAccountId(), "TRANSFER", amount, "Received from " + fromId);

            return "Transfer successful";
        });
    }

    // ───────── BALANCE ─────────
    public double calculateBalance(String accId) {

        double balance = 0;

        for (String line : FileService.getUserTransactions(accId)) {
            String[] p = line.split(",");
            String type = p[1];
            double amount = Double.parseDouble(p[2]);

            switch (type) {
                case "DEPOSIT": balance += amount; break;
                case "WITHDRAWAL": balance -= amount; break;
                case "TRANSFER":
                    if (p[3].contains("Sent")) balance -= amount;
                    else balance += amount;
                    break;
            }
        }

        return balance;
    }

    public Future<String> transferByAccountNumber(String accountId, String toAccountNumber, double amount) {

        return executor.submit(() -> {

            Account from = accounts.get(accountId);
            Account to = getByAccountNumber(toAccountNumber);

            if (from == null)
                return "ERROR: Sender account not found";

            if (to == null)
                return "ERROR: Recipient account not found";

            if (from.getAccountNumber().equals(toAccountNumber))
                return "ERROR: Cannot transfer to same account";

            try {
                // fraud check

                // withdraw from sender
                from.withdraw(amount);

                // deposit to receiver
                to.deposit(amount);

                // IMPORTANT: record proper transaction type
                from.getTransactionHistory().add(
                        new Transaction(
                                Transaction.Type.TRANSFER,
                                amount,
                                "Transfer to " + to.getAccountNumber()
                        )
                );

                to.getTransactionHistory().add(
                        new Transaction(
                                Transaction.Type.TRANSFER,
                                amount,
                                "Transfer from " + from.getAccountNumber()
                        )
                );

                // OPTIONAL: persist to file
                FileService.saveTransaction(
                        from.getAccountId(),
                        "TRANSFER",
                        amount,
                        "To " + toAccountNumber
                );

                FileService.saveTransaction(
                        to.getAccountId(),
                        "TRANSFER",
                        amount,
                        "From " + from.getAccountNumber()
                );

                return "Transferred MWK " + amount + " to " + to.getOwner();

            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        });
    }
}