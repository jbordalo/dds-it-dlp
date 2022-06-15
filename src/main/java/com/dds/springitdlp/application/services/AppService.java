package com.dds.springitdlp.application.services;

import com.dds.springitdlp.application.bftSmart.ConsensusClient;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.LedgerHandler;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.block.BlockRequest;
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
        return this.ledgerHandler.getBalance(new Account(accountId));
    }

    public List<Transaction> getExtract(String accountId) {
        return this.ledgerHandler.getExtract(new Account(accountId));
    }

    public double getTotalValue(List<String> accounts) {
        List<Account> accountsFinal = new LinkedList<>();
        for (String account : accounts) {
            accountsFinal.add(new Account(account));
        }
        return this.ledgerHandler.getTotalValue(accountsFinal);
    }

    public double getGlobalLedgerValue() {
        return this.ledgerHandler.getGlobalLedgerValue();
    }

    public Ledger getLedger() {
        return this.ledgerHandler.getLedger();
    }

    public Block getBlock(BlockRequest blockRequest) {
        // Verify blockRequest signature
        if (!BlockRequest.verify(blockRequest)) return null;
        // Send public key to ledgerHandler
        return this.ledgerHandler.getBlock(blockRequest.getAccount());
    }

    public boolean proposeBlock(Block block) {
        // Check block validity
        if (!Block.checkBlock(block)) return false;
        // TODO(currently broken due to reward transaction which is different) If local blockchain has the block, don't disseminate it
        if (this.ledgerHandler.hasBlock(block)) return false;

        return this.consensusClient.proposeBlock(block);
    }
}
