package com.dds.springitdlp.application.bftSmart;

import bftsmart.tom.ServiceProxy;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
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

    public void sendTransaction(Transaction transaction) throws ResponseStatusException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(transaction);
            byte[] bts = bos.toByteArray();
            byte[] reply = serviceProxy.invokeOrdered(bts);
            logger.log(Level.INFO, "sendTransaction@Client: sent transaction");

            if (reply != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

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

    public double getBalance(Account account) throws ResponseStatusException {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut)) {

            objOut.writeObject(LedgerRequestType.GET_BALANCE);
            objOut.writeObject(account);

            objOut.flush();
            byteOut.flush();

            byte[] reply = serviceProxy.invokeUnordered(byteOut.toByteArray());

            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                double balance = objIn.readDouble();

                if (balance == -1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

                return balance;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error while calculating balance", e);
        }
        return -1.0;
    }

    public List<Transaction> getExtract(Account account) {
        byte[] bytes = serviceProxy.invokeUnordered(null);
        List<Transaction> extract = new LinkedList<Transaction>();
        return extract;
    }

    public double getTotalValue(List<Account> accounts) {
        for (Account a : accounts) {
            logger.log(Level.INFO, a.toString());
        }
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut)) {

            objOut.writeObject(LedgerRequestType.GET_TOTAL_VALUE);
            objOut.writeObject(accounts);

            objOut.flush();
            byteOut.flush();

            byte[] reply = serviceProxy.invokeUnordered(byteOut.toByteArray());

            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                double totalValue = objIn.readDouble();

                if (totalValue == -1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

                return totalValue;
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error while calculating balance", e);
        }
        return -1.0;
    }

    public double getGlobalLedgerValue() {
        return 1.0;
    }
}
