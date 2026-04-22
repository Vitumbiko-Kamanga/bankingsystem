package bank.exception;

public class FraudDetectedException extends Exception {
    public FraudDetectedException(String message) {
        super(message);
    }
}