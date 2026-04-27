package bank.model;

import bank.exception.InsufficientFundsException;

public class SavingsAccount extends Account {

    private final double minBalance;

    public SavingsAccount(String accountId, String accountNumber,
                          String owner, double initialBalance) {
        this(accountId, accountNumber, owner, initialBalance, 100.0);
    }

    public SavingsAccount(String accountId, String accountNumber,
                          String owner, double initialBalance, double minBalance) {
        super(accountId, accountNumber, owner, initialBalance);
        this.minBalance = minBalance;
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        lock.lock();
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Withdrawal must be positive");
            }

            double newBalance = balance - amount;
            if (newBalance < minBalance) {
                throw new InsufficientFundsException(
                        "Cannot go below minimum balance of MWK " + minBalance
                );
            }

            balance = newBalance;

            Transaction t = new Transaction(
                    Transaction.Type.WITHDRAWAL, amount, "Savings Withdrawal"
            );
            transactionHistory.add(t);
            // NO FileService.saveTransaction() here!

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deposit(double amount) {
        super.deposit(amount);  // Use parent deposit (no file saving)
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }

    public double getMinBalance() {
        return minBalance;
    }
}