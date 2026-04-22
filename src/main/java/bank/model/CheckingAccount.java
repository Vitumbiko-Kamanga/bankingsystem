package bank.model;

import bank.exception.InsufficientFundsException;
import bank.service.FileService;

public class CheckingAccount extends Account {

    private final double overdraftLimit;

    public CheckingAccount(String accountId, String accountNumber,
                           String owner, double initialBalance,
                           double overdraftLimit) {
        super(accountId, accountNumber, owner, initialBalance);
        this.overdraftLimit = overdraftLimit;
    }

    // ─────────────────────────────
    // WITHDRAW (OVERRIDDEN)
    // ─────────────────────────────
    @Override
    public void withdraw(double amount) throws InsufficientFundsException {

        lock.lock();
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Withdrawal must be positive");
            }

            // allow overdraft
            if (amount > balance + overdraftLimit) {
                throw new InsufficientFundsException(
                        "Exceeded overdraft limit. Limit = MWK " + overdraftLimit
                );
            }

            balance -= amount;

            Transaction t = new Transaction(
                    Transaction.Type.WITHDRAWAL,
                    amount,
                    "Checking Withdrawal"
            );

            transactionHistory.add(t);

            // ✅ SAVE TO FILE
            FileService.saveTransaction(
                    accountId,
                    t.getType().toString(),
                    t.getAmount(),
                    t.getDescription()
            );

        } finally {
            lock.unlock();
        }
    }

    // ─────────────────────────────
    // DEPOSIT (OPTIONAL OVERRIDE)
    // ─────────────────────────────
    @Override
    public void deposit(double amount) {

        lock.lock();
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Deposit must be positive");
            }

            balance += amount;

            Transaction t = new Transaction(
                    Transaction.Type.DEPOSIT,
                    amount,
                    "Checking Deposit"
            );

            transactionHistory.add(t);

            // ✅ SAVE TO FILE
            FileService.saveTransaction(
                    accountId,
                    t.getType().toString(),
                    t.getAmount(),
                    t.getDescription()
            );

        } finally {
            lock.unlock();
        }
    }

    // ─────────────────────────────
    // ACCOUNT TYPE
    // ─────────────────────────────
    @Override
    public String getAccountType() {
        return "Checking";
    }

    // ─────────────────────────────
    // GET OVERDRAFT LIMIT
    // ─────────────────────────────
    public double getOverdraftLimit() {
        return overdraftLimit;
    }
}