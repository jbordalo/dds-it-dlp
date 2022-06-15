package com.dds.springitdlp.application.ledger;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.ProposeResult;
import com.dds.springitdlp.application.entities.results.TransactionResult;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.block.BlockHeader;
import com.dds.springitdlp.cryptography.Cryptography;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class LedgerHandler {
    private Ledger ledger;
    private final List<Transaction> transactionPool;
    private final String ledgerPath;
    private final Logger logger;

    public LedgerHandler() throws IOException {
        this.ledger = new Ledger();

        this.transactionPool = new LinkedList<>();

        this.logger = Logger.getLogger(LedgerHandler.class.getName());

        Files.createDirectories(Path.of(System.getenv("STORAGE_PATH")));

        this.ledgerPath = "ledger" + System.getenv("REPLICA_ID");
    }

    /**
     * Wrapper function for the Ledger sendTransaction,
     * persists data if there are no errors.
     *
     * @param transaction transaction to be handled
     * @return true if there was an error, false otherwise
     */
    public TransactionResult sendTransaction(Transaction transaction) {
        this.logger.log(Level.INFO, "sendTransaction@Server: " + transaction.toString());

        if (transaction.getAmount() <= 0 || !Transaction.verify(transaction) ||
                !this.ledger.hasBalance(transaction.getOrigin(), transaction.getAmount()))
            return TransactionResult.FAILED_TRANSACTION;

        if (transactionPool.contains(transaction) || this.ledger.transactionInLedger(transaction)) return TransactionResult.REPEATED_TRANSACTION;

        transactionPool.add(transaction);

        this.persist();

        return TransactionResult.OK_TRANSACTION;
    }

    private void persist() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos); FileOutputStream outputStream = new FileOutputStream(System.getenv("STORAGE_PATH") + this.ledgerPath)) {
            oos.writeObject(this.ledger);
            this.logger.log(Level.INFO, "persist@Server: persisting ledger");
            outputStream.write(bos.toByteArray());
        } catch (IOException e) {
            this.logger.log(Level.SEVERE, "persist@Server: error while persisting ledger");
            e.printStackTrace();
        }
    }

    public Ledger getLedger() {
        return this.ledger;
    }

    public double getBalance(Account account) {
        return this.ledger.getBalance(account);
    }

    public double getGlobalLedgerValue() {
        return this.ledger.getGlobalValue();
    }

    public List<Transaction> getExtract(Account account) {
        return this.ledger.getExtract(account);
    }

    public double getTotalValue(List<Account> list) {
        double total = 0.0;

        for (Account a : list) {
            total += this.ledger.getBalance(a);
        }

        return total;
    }

    public void setLedger(Ledger ledger) {
        this.ledger = ledger;
    }

    public Block getBlock(Account account) {

        Block lastBlock = this.ledger.getLastBlock();

        // If blockchain is empty we'll mine the genesis block
        if (lastBlock == null) {
            Transaction rewardTransaction = Transaction.REWARD_TRANSACTION(account);
            return Block.genesisBlock(rewardTransaction);
        }

        if (this.transactionPool.size() < Block.MIN_TRANSACTIONS_BLOCK - 1) return null;

        List<Transaction> transactions = this.transactionPool.subList(0, Block.MIN_TRANSACTIONS_BLOCK - 1);

        // this is the reward transaction
        transactions.add(Transaction.REWARD_TRANSACTION(account));

        return new Block(Cryptography.hash(lastBlock.toString()), BlockHeader.DEFAULT_DIFFICULTY, new ArrayList<>(transactions));
    }

    public ProposeResult proposeBlock(Block block) {
        if (Block.checkBlock(block) && !this.hasBlock(block)) {
            this.ledger.addBlock(block);

            for (Transaction transaction : block.getTransactions()) {
                this.transactionPool.remove(transaction);
            }

            return ProposeResult.BLOCK_ACCEPTED;
        }
        return ProposeResult.BLOCK_REJECTED;
    }

    public boolean hasBlock(Block block) {
        return this.ledger.hasBlock(block);
    }
}