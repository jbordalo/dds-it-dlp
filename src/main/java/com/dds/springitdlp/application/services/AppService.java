package com.dds.springitdlp.application.services;

import com.dds.springitdlp.application.bftSmart.LedgerClient;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class AppService {
    private final LedgerClient ledgerClient;

    @Autowired
    public AppService(LedgerClient ledgerClient) {
        this.ledgerClient = ledgerClient;
    }

    public void sendTransaction(Transaction transaction) {
        this.ledgerClient.sendTransaction(transaction);
    }

    public double getBalance(String accountId) {
        return this.ledgerClient.getBalance(new Account(accountId));
    }

    public List<Transaction> getExtract(String accountId) {
        return this.ledgerClient.getExtract(new Account(accountId));
    }

    public double getTotalValue(List<String> accounts) {
        List<Account> accountsFinal = new LinkedList<>();
        for (String account : accounts) {
            accountsFinal.add(new Account(account));
        }
        return this.ledgerClient.getTotalValue(accountsFinal);
    }

    public double getGlobalLedgerValue() {
        return this.ledgerClient.getGlobalLedgerValue();
    }

    public Ledger getLedger() {
        return this.ledgerClient.getLedger();
    }
}
