package bank.model;

import bank.exception.InsufficientFundsException;

public class CheckingAccount extends Account {

    private final double overdraftLimit;

    public CheckingAccount(String accountId, String accountNumber,
                           String owner, double initialBalance,
                           double overdraftLimit) {
        super(accountId, accountNumber, owner, initialBalance);
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    public void withdraw(double amount) throws InsufficientFundsException {
        lock.lock();
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Withdrawal must be positive");
            }

            //  Allow overdraft
            if (amount > balance + overdraftLimit) {
                throw new InsufficientFundsException(
                        "Exceeded overdraft limit. Limit = MWK " + overdraftLimit +
                                " | Balance = MWK " + balance
                );
            }

            balance -= amount;

            Transaction t = new Transaction(
                    Transaction.Type.WITHDRAWAL, amount, "Checking Withdrawal"
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
        return "Checking";
    }

    public double getOverdraftLimit() {
        return overdraftLimit;
    }
}