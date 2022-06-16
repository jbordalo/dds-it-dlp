package com.dds.springitdlp.application.services;

import com.dds.springitdlp.application.bftSmart.ConsensusClient;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.ProposeResult;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.LedgerHandler;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.block.BlockRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public TransactionResultStatus sendTransaction(Transaction transaction) {
        if (Transaction.verify(transaction)) {
            return this.consensusClient.sendTransaction(transaction).getResult();
        }
        return null;
    }

    public TransactionResultStatus sendAsyncTransaction(Transaction transaction) {
        if (Transaction.verify(transaction)) {
            return this.consensusClient.sendAsyncTransaction(transaction).getResult();
        }
        return null;
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

    public ProposeResult proposeBlock(Block block) {
        // Check block validity
        if (!Block.checkBlock(block)) return ProposeResult.BLOCK_REJECTED;
        // If local blockchain has the block, don't disseminate it
        if (this.ledgerHandler.hasBlock(block)) return ProposeResult.BLOCK_REJECTED;

        return this.consensusClient.proposeBlock(block);
    }
}
