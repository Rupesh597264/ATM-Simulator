package com.atm.banking;

import com.atm.transactions.Transaction;
// Account class represents a single bank account.
// Demonstrates: Encapsulation (private fields), Getters/Setters
public class Account {
    // Private fields - keep data hidden from outside classes
    private String accountNumber;
    private String holderName;
    private String pin;
    private double balance;

    private Transaction[] miniStatement;
    private int stmtIndex;
    // Constructor - called when creating an Account object
    public Account(String accountNumber, String holderName, String pin,
                   double balance, int stmtCapacity) {
        this.accountNumber = accountNumber; // Assign account number
        this.holderName = holderName;       // Assign account holder's name
        this.pin = pin;                     // Set the PIN
        this.balance = balance;             // Set initial balance
        this.miniStatement = new Transaction[stmtCapacity]; //Set ministatement
        this.stmtIndex = 0;                 //Set statement index to 0
    }
    // Getter methods provide read-only access to private variables
    public String getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }
    // Checks if entered PIN is correct
    public boolean validatePin(String attempt) {
        return this.pin.equals(attempt);
    }
    // Deposit amount
    public synchronized void deposit(double amount) { this.balance += amount; }
    // Withdraw amount
    public synchronized void withdraw(double amount) { this.balance -= amount; }
    // Returns current balance
    public double getBalance() { return this.balance; }
    // Adds a transaction to mini-statement
    public void addTransaction(Transaction tx) {
        // If full → remove oldest
        if (stmtIndex >= miniStatement.length) {
            // Shift all left by one
            for (int i = 1; i < miniStatement.length; i++)
                miniStatement[i-1] = miniStatement[i];
            // Insert new at last
            miniStatement[miniStatement.length - 1] = tx;
        } else {
            // Add new transaction normally
            miniStatement[stmtIndex++] = tx;
        }
    }
    // Returns recent transactions
    public Transaction[] getMiniStatement() {
        int filled = Math.min(stmtIndex, miniStatement.length);
        Transaction[] copy = new Transaction[filled];
        for (int i = 0; i < filled; i++) copy[i] = miniStatement[i];
        return copy;
    }
    // Convert account to CSV line
    public String toCSV() {
        return accountNumber + "," + holderName.replace(","," ") + "," + pin + "," + balance;
    }
    // Create account object from CSV line
    public static Account fromCSV(String line, int capacity) throws Exception {
        String[] p = line.split(",");
        if (p.length < 4) throw new Exception("Invalid CSV: " + line);
        return new Account(p[0].trim(), p[1].trim(), p[2].trim(), Double.parseDouble(p[3].trim()), capacity);
    }
}
