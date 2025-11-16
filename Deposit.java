package com.atm.transactions;

import com.atm.banking.Account;
import java.time.LocalDateTime;

public class Deposit extends Transaction {
    public Deposit(double amount) {
        super(amount, "DEPOSIT");
    }
    public Deposit(double amount, LocalDateTime ts) {
        super(amount, "DEPOSIT", ts);
    }

    @Override
    public void process(Account account) {
        account.deposit(amount);
        account.addTransaction(this);
    }
}
