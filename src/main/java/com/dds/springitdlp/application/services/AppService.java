package com.dds.springitdlp.application.services;

import com.dds.springitdlp.application.consensus.ConsensusPlane;
import com.dds.springitdlp.application.contracts.Endorser;
import com.dds.springitdlp.application.contracts.SmartContract;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.AsyncTransactionResult;
import com.dds.springitdlp.application.entities.results.ProposeResult;
import com.dds.springitdlp.application.entities.results.TransactionResult;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.LedgerHandler;
import com.dds.springitdlp.application.ledger.LedgerHandlerConfig;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.block.BlockRequest;
import com.dds.springitdlp.cryptography.Cryptography;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;

@Service
public class AppService {
    private final ConsensusPlane consensusClient;
    private final LedgerHandler ledgerHandler;
    private final LedgerHandlerConfig config;

    private final Endorser endorser;

    @Autowired
    public AppService(ConsensusPlane consensusClient, LedgerHandler ledgerHandler, LedgerHandlerConfig ledgerHandlerConfig, Endorser endorser) {
        this.consensusClient = consensusClient;
        this.ledgerHandler = ledgerHandler;
        this.config = ledgerHandlerConfig;
        this.endorser = endorser;

    }

    public TransactionResultStatus sendTransaction(Transaction transaction) {
        if (Transaction.verify(transaction)) {
            return this.consensusClient.sendTransaction(transaction).getResult();
        }
        return null;
    }

    public AsyncTransactionResult sendAsyncTransaction(Transaction transaction) {
        if (Transaction.verify(transaction)) {
            List<TransactionResult> results = this.consensusClient.sendAsyncTransaction(transaction);
            String signature = Cryptography.sign(results.toString(), this.config.getPrivateKey());
            return new AsyncTransactionResult(results, signature);
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

    public SmartContract endorse(byte[] smartContract) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(smartContract);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {

            SmartContract contract = (SmartContract) objIn.readObject();

            return this.endorser.endorse(contract);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
