package com.dds.springitdlp.application.services;

import com.dds.springitdlp.application.bftSmart.ConsensusClient;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.LedgerHandler;
import com.dds.springitdlp.application.ledger.block.Block;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedList;
import java.util.List;

@Service
public class AppService {
    private final ConsensusClient consensusClient;
    private final LedgerHandler ledgerHandler;

    @Autowired
    public AppService(ConsensusClient consensusClient, LedgerHandler ledgerHandler) {
        this.consensusClient = consensusClient;
        this.ledgerHandler = ledgerHandler;
    }

    public void sendTransaction(Transaction transaction) {
        if (Transaction.verify(transaction)) {
            this.consensusClient.sendTransaction(transaction);
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public void sendAsyncTransaction(Transaction transaction) {
        if (Transaction.verify(transaction)) {
            this.consensusClient.sendAsyncTransaction(transaction);
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public double getBalance(String accountId) {
        return this.consensusClient.getBalance(new Account(accountId));
    }

    public List<Transaction> getExtract(String accountId) {
        return this.consensusClient.getExtract(new Account(accountId));
    }

    public double getTotalValue(List<String> accounts) {
        List<Account> accountsFinal = new LinkedList<>();
        for (String account : accounts) {
            accountsFinal.add(new Account(account));
        }
        return this.consensusClient.getTotalValue(accountsFinal);
    }

    public double getGlobalLedgerValue() {
        return this.consensusClient.getGlobalLedgerValue();
    }

    public Ledger getLedger() {
        return this.consensusClient.getLedger();
    }

    public Block getBlock() {
        return this.ledgerHandler.getBlock();
    }

    public boolean proposeBlock(Block block) {
        return this.consensusClient.proposeBlock(block);
    }
}
