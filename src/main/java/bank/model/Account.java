package bank.model;

import bank.exception.InsufficientFundsException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Account {

    protected final String accountId;
    protected final String accountNumber;
    protected final String owner;
    protected double balance;
    protected final List<Transaction> transactionHistory = new ArrayList<>();
    protected final ReentrantLock lock = new ReentrantLock();

    public Account(String accountId, String accountNumber, String owner, double balance) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.balance = balance;
    }

    // FIXED: NO FILE SAVING HERE - BankService handles it
    public void deposit(double amount) {
        lock.lock();
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Deposit must be positive");
            }
            balance += amount;

            Transaction t = new Transaction(Transaction.Type.DEPOSIT, amount, "Deposit");
            transactionHistory.add(t);
            // ✅ NO FileService.saveTransaction() here!

        } finally {
            lock.unlock();
        }
    }

    // FIXED: NO FILE SAVING HERE - BankService handles it
    public void withdraw(double amount) throws InsufficientFundsException {
        lock.lock();
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Withdrawal must be positive");
            }
            if (amount > balance) {
                throw new InsufficientFundsException("Insufficient balance: MWK " + balance);
            }

            balance -= amount;

            Transaction t = new Transaction(Transaction.Type.WITHDRAWAL, amount, "Withdrawal");
            transactionHistory.add(t);
            // NO FileService.saveTransaction() here!

        } finally {
            lock.unlock();
        }
    }

    // Getters
    public String getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public String getOwner() { return owner; }
    public double getBalance() { return balance; }
    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    public abstract String getAccountType();
}