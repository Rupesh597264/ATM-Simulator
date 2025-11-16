package com.atm.transactions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.atm.banking.Account;

public abstract class Transaction {
    public double amount;
    protected LocalDateTime timestamp;
    public String type;

    public Transaction(double amount, String type) {
        this(amount, type, LocalDateTime.now());
    }

    public Transaction(double amount, String type, LocalDateTime timestamp) {
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
    }

    public abstract void process(Account account) throws Exception;

    public String summary() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "[" + timestamp.format(f) + "] " + type + ": " + String.format("%.2f", amount);
    }

    public LocalDateTime getTimestamp() { return timestamp; }
}
