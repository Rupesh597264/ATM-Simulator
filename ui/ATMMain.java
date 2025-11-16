package com.atm.ui;

import com.atm.banking.Account;
import com.atm.transactions.*;
import com.atm.security.InvalidPinException;

import java.io.*; // to read and write files
import java.nio.file.*;  //for checking if a file exists and to create new files easily
import java.time.LocalDateTime; //to get current date and time
import java.time.format.DateTimeFormatter; //to get local date and time into a readable form 
import java.util.*;

public class ATMMain {

    // Array to store all account objects
    private static Account[] accounts = new Account[0];

    // File names for saving accounts and transaction history
    private static final String ACCOUNTS_FILE = "accounts.csv";
    private static final String HISTORY_FILE = "transaction_history.csv";

    // Maximum mini-statement size
    private static final int MINI_CAPACITY = 20;

    // Timestamp formatter for saving/loading history
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static void main(String[] args) {

        loadAccounts(); // Load accounts from CSV
        loadHistory();  // Load previous transactions

        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("Welcome to ATM Simulator\n");

            while (true) {
                // Ask for account number
                System.out.print("Enter account number (or 'exit'): ");
                String accNo = sc.nextLine().trim();
                if (accNo.equalsIgnoreCase("exit")) break;

                Account account = findAccount(accNo); // Search account

                if (account == null) {
                    System.out.println("Account not found. Try again.\n");
                    continue;
                }

                // PIN authentication
                try {
                    authenticate(account, sc);
                    System.out.println("Welcome, " + account.getHolderName() + "!\n");
                } catch (InvalidPinException e) {
                    System.out.println(e.getMessage());
                    continue;
                }

                // Main transaction menu
                boolean sessionActive = true;
                while (sessionActive) {
                    System.out.println("--- Transaction Menu ---");
                    System.out.println("1. Check Balance");
                    System.out.println("2. Deposit");
                    System.out.println("3. Withdraw");
                    System.out.println("4. Mini-statement");
                    System.out.println("5. Logout");
                    System.out.print("Choose an option: ");
                    String choice = sc.nextLine().trim();

                    switch (choice) {
                        case "1":
                            System.out.printf("Current balance: %.2f\n", account.getBalance());
                            break;

                        case "2":
                            handleDeposit(account, sc); // Process deposit
                            persistAccountsToFile();     // Save updated balance
                            break;

                        case "3":
                            handleWithdrawal(account, sc); // Process withdrawal
                            persistAccountsToFile();
                            break;

                        case "4":
                            showMiniStatement(account); // Display last transactions
                            break;

                        case "5":
                            sessionActive = false;
                            System.out.println("Logged out.\n");
                            break;

                        default:
                            System.out.println("Invalid option. Try again.");
                    }
                }
            }
            System.out.println("Thank you for using ATM Simulator. Goodbye!");
        }
    }

    // ---------------- Authentication ----------------

    // PIN verification (max 3 attempts)
    private static void authenticate(Account account, Scanner sc) throws InvalidPinException {
        int attempts = 0;

        while (attempts < 3) {
            System.out.print("Enter 4-digit PIN: ");
            String pin = sc.nextLine().trim();

            // PIN must be 4 digits
            if (!pin.matches("\\d{4}")) {
                attempts++;
                System.out.println("PIN must be 4 digits. Attempts left: " + (3 - attempts));
                continue;
            }

            // Validate PIN
            if (account.validatePin(pin)) return;

            attempts++;
            System.out.println("Invalid PIN. Attempts left: " + (3 - attempts));
        }

        throw new InvalidPinException("Too many invalid PIN attempts. Returning to account selection.\n");
    }

    // ---------------- Deposit ----------------

    // Deposit handler
    private static void handleDeposit(Account account, Scanner sc) {
        System.out.print("Enter amount to deposit: ");
        String amtStr = sc.nextLine().trim();

        try {
            double amt = Double.parseDouble(amtStr);

            if (amt <= 0) {
                System.out.println("Amount must be positive.");
                return;
            }

            // Create deposit transaction
            Transaction tx = new Deposit(amt);
            tx.process(account);
            appendHistory(account.getAccountNumber(), tx);

            System.out.printf("Deposited %.2f successfully. New balance: %.2f\n", amt, account.getBalance());

        } catch (NumberFormatException e) {
            System.out.println("Invalid numeric input for amount.");
        } catch (Exception e) {
            System.out.println("Deposit failed: " + e.getMessage());
        }
    }

    // ---------------- Withdrawal ----------------

    private static void handleWithdrawal(Account account, Scanner sc) {
        System.out.print("Enter amount to withdraw: ");
        String amtStr = sc.nextLine().trim();

        try {
            double amt = Double.parseDouble(amtStr);

            if (amt <= 0) {
                System.out.println("Amount must be positive.");
                return;
            }

            // Limit check
            if (amt > 50000) {
                System.out.println("Withdrawal exceeds single-transaction limit of 50000.");
                return;
            }

            Transaction tx = new Withdrawal(amt);

            try {
                tx.process(account);
                appendHistory(account.getAccountNumber(), tx);
                System.out.printf("Withdrawn %.2f successfully. New balance: %.2f\n", amt, account.getBalance());
            } catch (Exception e) {
                System.out.println("Withdrawal failed: " + e.getMessage());
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid numeric input for amount.");
        }
    }

    // ---------------- Mini Statement ----------------

    private static void showMiniStatement(Account account) {
        Transaction[] stmts = account.getMiniStatement();

        if (stmts.length == 0) {
            System.out.println("No transactions yet.");
            return;
        }

        System.out.println("Mini-statement (most recent first):");

        // Print from latest to oldest
        for (int i = stmts.length - 1; i >= 0; i--) {
            System.out.println(stmts[i].summary());
        }
    }

    // ---------------- File I/O ----------------

    // Load accounts from CSV
    private static void loadAccounts() {
        Path p = Paths.get(ACCOUNTS_FILE);

        if (!Files.exists(p)) {
            System.out.println("accounts.csv not found. Creating default accounts.");
            createProvidedAccountsFile();
        }

        try {
            List<String> lines = Files.readAllLines(p);
            List<String> clean = new ArrayList<>();

            for (String l : lines)
                if (!l.trim().isEmpty()) clean.add(l);

            accounts = new Account[clean.size()];

            for (int i = 0; i < clean.size(); i++) {
                accounts[i] = Account.fromCSV(clean.get(i), MINI_CAPACITY);
            }

            System.out.println("Loaded " + accounts.length + " accounts.\n");

        } catch (Exception e) {
            System.out.println("Failed to load accounts: " + e.getMessage());
            accounts = new Account[0];
        }
    }

    // Save updated account balances
    private static void persistAccountsToFile() {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(ACCOUNTS_FILE))) {
            for (Account a : accounts)
                bw.write(a.toCSV() + System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Failed to persist accounts: " + e.getMessage());
        }
    }

    // Create sample accounts when CSV doesn't exist
    private static void createProvidedAccountsFile() {
        String[] sample = {
            "1001,Rupesh Saini,1234,176381",
            "1002,Ansh Rana,2345,50087",
            "1003,Monish Yadav,3456,17393",
            "1004,Tanishq Kapil,4567,80980",
            "1005,Mridul Sharma,5678,20500",
            "1006,Maulik Chopra,6789,49070"
        };

        try {
            Files.write(Paths.get(ACCOUNTS_FILE), Arrays.asList(sample));
        } catch (IOException e) {
            System.out.println("Could not create accounts file: " + e.getMessage());
        }
    }

    // Append transaction history to file
    private static void appendHistory(String accountId, Transaction tx) {
        String line = String.join(",",
                accountId,
                tx.type,
                String.format(Locale.US, "%.2f", tx.amount),
                tx.getTimestamp().format(TF));

        try {
            Files.write(Paths.get(HISTORY_FILE),
                    Arrays.asList(line),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

        } catch (IOException e) {
            System.out.println("Failed to write history: " + e.getMessage());
        }
    }

    // Load previous history to rebuild mini-statements
    private static void loadHistory() {
        Path p = Paths.get(HISTORY_FILE);

        if (!Files.exists(p)) return;

        try {
            List<String> lines = Files.readAllLines(p);

            for (String l : lines) {
                if (l.trim().isEmpty()) continue;

                String[] parts = l.split(",");
                if (parts.length < 4) continue;

                String accId = parts[0].trim();
                String type = parts[1].trim();
                double amt = Double.parseDouble(parts[2].trim());
                LocalDateTime ts = LocalDateTime.parse(parts[3].trim(), TF);

                Account acc = findAccount(accId);
                if (acc == null) continue;

                Transaction tx = null;

                if (type.equalsIgnoreCase("DEPOSIT"))
                    tx = new Deposit(amt, ts);
                else if (type.equalsIgnoreCase("WITHDRAWAL"))
                    tx = new Withdrawal(amt, ts);

                if (tx != null) acc.addTransaction(tx);
            }

        } catch (Exception e) {
            System.out.println("Failed to load history: " + e.getMessage());
        }
    }

    // Find account by account number
    private static Account findAccount(String accNo) {
        for (Account a : accounts)
            if (a.getAccountNumber().equals(accNo))
                return a;

        return null;
    }
}
