package com.dds.springitdlp.application.services;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.repositories.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AppService {
    private final Storage storage;

    @Autowired
    public AppService(Storage storage) {
        this.storage = storage;

    }

    public int sendTransaction(Transaction transaction) {
        return 0;
    }
}
