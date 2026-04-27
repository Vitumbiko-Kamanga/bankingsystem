package bank.service;

public class InputValidator {

    public static boolean isValidUsername(String username) {
        return username != null && username.matches("[a-zA-Z0-9_]{3,20}");
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidAmount(String input) {
        try {
            double amount = Double.parseDouble(input);
            return amount > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidAccountNumber(String accNo) {
        return accNo != null && accNo.matches("[A-Z0-9]{6,12}");
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
    }
}