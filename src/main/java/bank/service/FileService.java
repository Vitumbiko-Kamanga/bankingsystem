package bank.service;

import bank.model.Transaction;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class FileService {

    private static final String USERS        = "users.txt";
    private static final String ACCOUNTS     = "accounts.txt";
    private static final String TRANSACTIONS = "transactions.txt";
    private static final String ADMIN_FILE   = "admin.txt";

    //  Users
    public static void saveUser(String username, String password, String accId) {
        writeLine(USERS, username + "," + password + "," + accId);
    }

    public static List<String> readUsers() {
        return readFile(USERS);
    }

    public static boolean deleteUser(String username) {
        List<String> users = readUsers();
        List<String> updated = users.stream()
                .filter(line -> !line.startsWith(username + ","))
                .toList();

        if (updated.size() == users.size()) return false;

        try (FileWriter fw = new FileWriter(USERS, false)) {
            for (String line : updated) fw.write(line + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean updateUserPassword(String username, String newPassword) {
        List<String> users = readUsers();
        List<String> updated = new ArrayList<>();
        boolean found = false;

        for (String line : users) {
            String[] parts = line.split(",");
            if (parts.length >= 3 && parts[0].equals(username)) {
                updated.add(username + "," + newPassword + "," + parts[2]);
                found = true;
            } else {
                updated.add(line);
            }
        }

        if (!found) return false;

        try (FileWriter fw = new FileWriter(USERS, false)) {
            for (String line : updated) fw.write(line + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // ── Accounts ─────────────────────────────────────────────────────
    public static void saveAccount(String accId, String accNo, String owner) {
        writeLine(ACCOUNTS, accId + "," + accNo + "," + owner);
    }

    public static List<String> readAccounts() {
        return readFile(ACCOUNTS);
    }

    public static void deleteAccount(String accId) {
        rewriteFileExcluding(ACCOUNTS,      accId + ",");
        rewriteFileExcluding(TRANSACTIONS,  accId + ",");
    }

    // ── Transactions

    /** Appends one transaction line to transactions.txt */
    public static void saveTransaction(String accNo, String type, double amount, String desc) {
        String line = String.format("%s,%s,%.2f,%s,%d",
                accNo, type, amount,
                desc.replace(",", ";"),   // escape commas in description
                System.currentTimeMillis());
        writeLine(TRANSACTIONS, line);
    }

    /**
     * Loads every transaction from transactions.txt as Transaction objects,
     * sorted newest-first. Skips blank or malformed lines silently.
     */
    public static List<Transaction> getAllTransactionsAsObjects() {
        List<Transaction> transactions = new ArrayList<>();
        List<String> lines = readFile(TRANSACTIONS);

        System.out.println(" Reading " + lines.size() + " transaction lines...");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) continue;

            try {
                String[] parts = line.split(",", 5);
                if (parts.length < 5) {
                    System.err.println(" Invalid format line " + (i + 1) + ": " + line);
                    continue;
                }

                String           accId       = parts[0].trim();
                Transaction.Type type        = Transaction.Type.valueOf(parts[1].trim());
                double           amount      = Double.parseDouble(parts[2].trim());
                String           description = parts[3].trim().replace(";", ","); // unescape
                long             epochMillis = Long.parseLong(parts[4].trim());

                LocalDateTime timestamp = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(epochMillis),
                        ZoneId.systemDefault()
                );

                Transaction tx = new Transaction(type, amount, description, timestamp);
                tx.setAccountId(accId);
                transactions.add(tx);

            } catch (Exception e) {
                System.err.println("❌ Parse error line " + (i + 1) + ": " + line);
                e.printStackTrace();
            }
        }

        System.out.println("✅ Successfully parsed " + transactions.size() + " transactions:");
        transactions.forEach(t ->
                System.out.println("  " + t.getAccountId() + " | " + t.getType() +
                        " | MWK " + String.format("%.2f", t.getAmount()) +
                        " | " + t.getDescription())
        );

        return transactions.stream()
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .toList();
    }

    /**
     * Returns formatted transaction strings for a specific account,
     * sorted newest-first. Used for user-facing history displays.
     */
    public static List<String> getUserTransactions(String accId) {
        return getAllTransactionsAsObjects().stream()
                .filter(t -> t.getAccountId().equals(accId))
                .map(FileService::formatTransactionForUser)
                .collect(Collectors.toList());
    }

    private static String formatTransactionForUser(Transaction t) {
        String emoji = switch (t.getType()) {
            case DEPOSIT    -> "💰";
            case WITHDRAWAL -> "💸";
            case TRANSFER   -> "🔄";
        };
        return String.format("%s %s MWK %.2f | %s | %s",
                emoji, t.getType(), t.getAmount(),
                t.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                t.getDescription());
    }

    // ── Admin ─────────────────────────────────────────────────────────
    public static void saveAdminPassword(String password) {
        writeLine(ADMIN_FILE, password);
    }

    public static String readAdminPassword() {
        List<String> lines = readFile(ADMIN_FILE);
        return lines.isEmpty() ? "admin123" : lines.get(0).trim();
    }

    // ── File helpers ──────────────────────────────────────────────────
    private static void rewriteFileExcluding(String file, String prefix) {
        List<String> lines = readFile(file);
        try (FileWriter fw = new FileWriter(file, false)) {
            for (String line : lines) {
                if (!line.startsWith(prefix)) fw.write(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeLine(String file, String line) {
        try (FileWriter fw = new FileWriter(file, true)) {
            fw.write(line + "\n");
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> readFile(String file) {
        List<String> list = new ArrayList<>();
        File f = new File(file);
        if (!f.exists()) return list;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) list.add(line.trim());
            }
        } catch (IOException e) {
            System.out.println("📂 Cannot read file: " + file);
        }
        return list;
    }
}