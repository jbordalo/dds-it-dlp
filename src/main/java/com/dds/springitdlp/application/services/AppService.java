package com.dds.springitdlp.application.services;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.repositories.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class AppService {
    private final Storage storage;
    private LedgerClient ledgerClient;

    @Autowired
    public AppService(Storage storage) {
        this.storage = storage;
        this.ledgerClient = new LedgerClient();
    }

    public int sendTransaction(Transaction transaction) {
        ledgerClient.sendTransaction(transaction);
        return 0;
    }
}
