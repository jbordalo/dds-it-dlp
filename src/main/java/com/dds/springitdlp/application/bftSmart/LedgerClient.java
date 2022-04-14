package com.dds.springitdlp.application.bftSmart;

import bftsmart.tom.ServiceProxy;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class LedgerClient {
    ServiceProxy serviceProxy;
    Logger logger;

    public LedgerClient() {
        this.logger = Logger.getLogger(LedgerClient.class.getName());
        int id = Integer.parseInt(System.getenv().get("REPLICA_ID"));
        serviceProxy = new ServiceProxy(id);
    }

    public void sendTransaction(Transaction transaction) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(transaction);
            byte[] bts = bos.toByteArray();
            serviceProxy.invokeOrdered(bts);
            logger.log(Level.INFO, "sendTransaction@Client: sent transaction");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Ledger getLedger() {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut)) {

            objOut.writeObject(LedgerRequestType.GET_LEDGER);

            objOut.flush();
            byteOut.flush();

            byte[] reply = serviceProxy.invokeUnordered(byteOut.toByteArray());

            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                return (Ledger) objIn.readObject();
        }
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "error while retrieving ledger", e);
        }
        return null;
    }

    public double getBalance(Account account) {
        byte[] bytes = serviceProxy.invokeUnordered(null);
        return 1.0;
    }

    public List<Transaction> getExtract(Account account) {
        byte[] bytes = serviceProxy.invokeUnordered(null);
        List<Transaction> extract = new LinkedList<Transaction>();
        return extract;
    }

    public double getTotalValue(List<Account> accounts) {
        for (Account a: accounts) {
            logger.log(Level.INFO, a.toString());
        }
        return 1.0;
    }

    public double getGlobalLedgerValue() {
        return 1.0;
    }
}
