package bank.service;

import bank.exception.FraudDetectedException;
import bank.model.Account;
import bank.model.Transaction;
import java.util.List;

public class FraudDetectionService {
    private static final double MAX_AMOUNT = 10000.00;
    private static final int RAPID_TRANSACTION_LIMIT = 3;
    private static final int RAPID_TRANSACTION_SECONDS = 10;

    public void check(Account account, double amount) throws FraudDetectedException {
        checkLargeTransaction(amount);
        checkRapidTransactions(account);
    }

    private void checkLargeTransaction(double amount) throws FraudDetectedException {
        if (amount > MAX_AMOUNT)
            throw new FraudDetectedException(
                    "Large transaction flagged: MWK " + amount + " exceeds threshold of " +  MAX_AMOUNT);
    }

    private void checkRapidTransactions(Account account) throws FraudDetectedException {
        List<Transaction> history = account.getTransactionHistory();
        if (history.size() < RAPID_TRANSACTION_LIMIT) return;

        List<Transaction> recent = history.subList(
                history.size() - RAPID_TRANSACTION_LIMIT, history.size());

        long seconds = java.time.Duration.between(
                recent.get(0).getTimestamp(),
                recent.get(recent.size() - 1).getTimestamp()
        ).getSeconds();

        if (seconds <= RAPID_TRANSACTION_SECONDS)
            throw new FraudDetectedException("Rapid transactions detected! Possible fraud.");
    }
}