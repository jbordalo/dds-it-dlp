package com.dds.springitdlp.application.ledger;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.block.Block;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class LedgerHandler {

    private Ledger ledger;
    private final String ledgerPath;
    private final Logger logger;

    public LedgerHandler() throws IOException {
        this.ledger = new Ledger();

        this.logger = Logger.getLogger(LedgerHandler.class.getName());

        Files.createDirectories(Path.of(System.getenv("STORAGE_PATH")));

        this.ledgerPath = "ledger" + System.getenv("REPLICA_ID");
    }

    /**
     * Wrapper function for the Ledger sendTransaction,
     * persists data if there are no errors.
     *
     * @param transaction
     * @return true if there was an error, false otherwise
     */
    public boolean sendTransaction(Transaction transaction) {
        this.logger.log(Level.INFO, "sendTransaction@Server: " + transaction.toString());

        boolean error = !this.ledger.sendTransaction(transaction);

        if (!error) this.persist();

        return error;
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
        return this.getTotal(this.ledger.getMap().keySet().stream().toList());
    }

    public List<Transaction> getExtract(Account account) {
        return this.ledger.getExtract(account);
    }

    public double getTotalValue(List<Account> list) {
        return this.getTotal(list);
    }

    public double getTotal(List<Account> list) {
        double total = 0.0;
        for (Account a : list) {
            total += Math.max(this.ledger.getBalance(a), 0.0);
        }
        return total;
    }

    public void setLedger(Ledger ledger) {
        this.ledger = ledger;
    }

    public Block getBlock() {
        return this.ledger.getBlock();
    }

    public boolean proposeBlock(Block block) {
        if (Block.checkBlock(block) && !this.hasBlock(block)) {
            this.ledger.addBlock(block);
            return true;
        }
        return false;
    }

    public boolean hasBlock(Block block) {
        return this.ledger.hasBlock(block);
    }
}