package com.dds.springitdlp.application.services;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.repositories.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class AppService {
    private final Storage storage;
    private LedgerServer ledgerServer;
    private LedgerClient ledgerClient;

    @Autowired
    public AppService(Storage storage) {
        this.storage = storage;
        Random r = new Random();
        int id = r.nextInt();
        //this.ledgerClient = new LedgerClient(id);
        this.ledgerServer = new LedgerServer(id);
    }

    public int sendTransaction(Transaction transaction) {
        return 0;
    }
}
