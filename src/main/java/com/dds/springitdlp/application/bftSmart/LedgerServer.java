package com.dds.springitdlp.application.bftSmart;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class LedgerServer extends DefaultSingleRecoverable implements CommandLineRunner {
    private final Logger logger;
    private Ledger ledger;

    public LedgerServer() {
        this.logger = Logger.getLogger(LedgerServer.class.getName());
        this.ledger = new Ledger();
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
            objOut.writeObject(ledger);
            return byteOut.toByteArray();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error while taking snapshot", e);
        }
        return new byte[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public void installSnapshot(byte[] state) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(state);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {
            ledger = (Ledger) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "error while installing snapshot", e);
        }
    }

    @Override
    public byte[] appExecuteOrdered(byte[] bytes, MessageContext messageContext) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInputStream ois = new ObjectInputStream(bis)) {
            Transaction transaction = (Transaction) ois.readObject();
            this.logger.log(Level.INFO, "sendTransaction@Server: transaction.toString()");

            return this.ledger.sendTransaction(transaction) == 0 ? null : new byte[0];
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getLedger() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this.ledger);
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
            oos.writeDouble(ledger.getBalance(account));
            logger.log(Level.INFO, "getBalance@Server: sending balance for " + account.getAccountId());
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
                case GET_BALANCE -> this.getBalance(objIn);
//                case GET_EXTRACT -> this.getExtract();
//                case GET_TOTAL_VALUE -> this.getTotalValue();
//                case GET_GLOBAL_LEDGER_VALUE -> this.getGlobalLedgerValue();
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

}
