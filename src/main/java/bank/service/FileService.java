package bank.service;

import java.io.*;
import java.util.*;

public class FileService {

    private static final String USERS = "users.txt";
    private static final String ACCOUNTS = "accounts.txt";
    private static final String TRANSACTIONS = "transactions.txt";

    // ───────── USERS ─────────
    public static void saveUser(String username, String password, String accId) {
        writeLine(USERS, username + "," + password + "," + accId);
    }

    public static List<String> readUsers() {
        return readFile(USERS);
    }

    // ───────── ACCOUNTS ─────────
    public static void saveAccount(String accId, String accNo, String owner) {
        writeLine(ACCOUNTS, accId + "," + accNo + "," + owner);
    }

    public static List<String> readAccounts() {
        return readFile(ACCOUNTS);
    }

    // ───────── TRANSACTIONS ─────────
    public static void saveTransaction(String accId, String type, double amount, String desc) {
        writeLine(TRANSACTIONS,
                accId + "," + type + "," + amount + "," + desc + "," + System.currentTimeMillis());
    }

    public static List<String> getUserTransactions(String accId) {
        List<String> all = readFile(TRANSACTIONS);
        List<String> result = new ArrayList<>();

        for (String line : all) {
            if (line.startsWith(accId + ",")) {
                result.add(line);
            }
        }
        return result;
    }

    public static List<String> getAllTransactions() {
        return readFile(TRANSACTIONS);
    }

    // ───────── HELPERS ─────────
    private static void writeLine(String file, String line) {
        try (FileWriter fw = new FileWriter(file, true)) {
            fw.write(line + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> readFile(String file) {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) list.add(line);
        } catch (IOException e) {
            System.out.println(file + " not found yet.");
        }
        return list;
    }
}