package com.dds.springitdlp.application.bftSmart;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public byte[] sendTransaction(ObjectInput objectInput) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            Transaction transaction = (Transaction) objectInput.readObject();
            this.logger.log(Level.INFO, "sendTransaction@Server: " + transaction.toString());

            boolean error = !this.ledger.sendTransaction(transaction);

            if (!error) this.persist();

            return error ? new byte[]{0x01} : new byte[]{0x00};
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private void persist() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos); FileOutputStream outputStream = new FileOutputStream(System.getenv("STORAGE_PATH") + this.ledgerPath)) {
            oos.writeObject(this.ledger);
            logger.log(Level.INFO, "persist@Server: persisting ledger");
            outputStream.write(bos.toByteArray());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "persist@Server: error while persisting ledger");
            e.printStackTrace();
        }
    }

    public byte[] getLedger() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this.ledger);
            oos.flush();
            bos.flush();

            this.logger.log(Level.INFO, "getLedger@Server: sending ledger");
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getBalance(ObjectInput objectInput) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            Account account = (Account) objectInput.readObject();
            oos.writeDouble(this.ledger.getBalance(account));
            oos.flush();

            this.logger.log(Level.INFO, "getBalance@Server: sending balance for " + account.getAccountId());
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public byte[] getGlobalLedgerValue() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeDouble(this.getTotal(this.ledger.getMap().keySet().stream().toList()));
            oos.flush();
            logger.log(Level.INFO, "getGlobalLedgerValue@Server");
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    byte[] getExtract(ObjectInput objectInput) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            Account account = (Account) objectInput.readObject();
            oos.writeObject(this.ledger.getExtract(account));
            oos.flush();
            this.logger.log(Level.INFO, "getExtract@Server: fetching extract of " + account.getAccountId());
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public byte[] getTotalValue(ObjectInput objectInput) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            List<Account> list = (List<Account>) objectInput.readObject();
            oos.writeDouble(this.getTotal(list));
            oos.flush();
            this.logger.log(Level.INFO, "getTotalValue@Server: sending total value of the given list");
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
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
}