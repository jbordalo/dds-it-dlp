package com.dds.springitdlp.application.services;

import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AppService {
    private final LedgerClient ledgerClient;

    @Autowired
    public AppService(LedgerClient ledgerClient) {
        this.ledgerClient = ledgerClient;
    }

    public int sendTransaction(Transaction transaction) {
        this.ledgerClient.sendTransaction(transaction);
        // TODO
        return 0;
    }

    public double getBalance(String accountId) {
        return this.ledgerClient.getBalance(accountId);
    }

    public Ledger getLedger() {
        return this.ledgerClient.getLedger();
    }

}
