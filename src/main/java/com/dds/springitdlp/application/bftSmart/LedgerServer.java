package com.dds.springitdlp.application.bftSmart;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class LedgerServer extends DefaultSingleRecoverable implements CommandLineRunner {
    private final Logger logger;
    private Ledger ledger;
    private final String ledgerPath;

    public LedgerServer() throws IOException {
        this.logger = Logger.getLogger(LedgerServer.class.getName());
        this.ledger = new Ledger();

        Files.createDirectories(Path.of(System.getenv("STORAGE_PATH")));

        this.ledgerPath = "ledger" + System.getenv("REPLICA_ID");
    }

    @Override
    public void run(String... args) throws Exception {
        int id = Integer.parseInt(System.getenv().get("REPLICA_ID"));
        new ServiceReplica(id, this, this);
    }

    @Override
    public byte[] getSnapshot() {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut)) {
            objOut.writeObject(this.ledger);
            return byteOut.toByteArray();
        } catch (IOException e) {
            this.logger.log(Level.SEVERE, "error while taking snapshot", e);
        }
        return new byte[0];
    }

    @Override
    public void installSnapshot(byte[] state) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(state);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {
            this.ledger = (Ledger) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            this.logger.log(Level.SEVERE, "error while installing snapshot", e);
        }
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

    @Override
    public byte[] appExecuteOrdered(byte[] bytes, MessageContext messageContext) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInputStream ois = new ObjectInputStream(bis)) {
            Transaction transaction = (Transaction) ois.readObject();
            this.logger.log(Level.INFO, "sendTransaction@Server: " + transaction.toString());

            boolean error = !this.ledger.sendTransaction(transaction);

            if (!error) this.persist();

            return error ? new byte[0] : null;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] getLedger() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this.ledger);
            this.logger.log(Level.INFO, "getLedger@Server: sending ledger");
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] getBalance(ObjectInput objectInput) {
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

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext messageContext) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {

            LedgerRequestType reqType = (LedgerRequestType) objIn.readObject();
            switch (reqType) {
                case GET_LEDGER -> {
                    return this.getLedger();
                }
                case GET_BALANCE -> {
                    return this.getBalance(objIn);
                }
                case GET_EXTRACT -> {
                    return this.getExtract(objIn);
                }
                case GET_TOTAL_VALUE -> {
                    return this.getTotalValue(objIn);
                }
                case GET_GLOBAL_LEDGER_VALUE -> {
                    return this.getGlobalLedgerValue();
                }
                default -> {
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private byte[] getGlobalLedgerValue() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeDouble(this.getTotal(this.ledger.getMap().keySet().stream().toList()));
            oos.flush();
            this.logger.log(Level.INFO, "getGlobalLedgerValue@Server");
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] getExtract(ObjectInput objectInput) {
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
    private byte[] getTotalValue(ObjectInput objectInput) {
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

    private double getTotal(List<Account> list) {
        double total = 0.0;
        for (Account a : list) {
            total += Math.max(this.ledger.getBalance(a), 0.0);
        }
        return total;
    }

}
