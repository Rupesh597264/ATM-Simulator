package com.atm.transactions;

import com.atm.banking.Account;
import java.time.LocalDateTime;

public class Withdrawal extends Transaction {
    public Withdrawal(double amount) {
        super(amount, "WITHDRAWAL");
    }
    public Withdrawal(double amount, LocalDateTime ts) {
        super(amount, "WITHDRAWAL", ts);
    }

    @Override
    public void process(Account account) throws Exception {
        if (account.getBalance() < amount)
            throw new Exception("Insufficient funds");

        account.withdraw(amount);
        account.addTransaction(this);
    }
}
