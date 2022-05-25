package com.dds.springitdlp.application.bftSmart;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class LedgerClient {
    AsynchServiceProxy serviceProxy;
    Logger logger;

    public LedgerClient() {
        this.logger = Logger.getLogger(LedgerClient.class.getName());
        int id = Integer.parseInt(System.getenv().get("REPLICA_ID"));
        serviceProxy = new AsynchServiceProxy(id);
    }

    public void sendTransaction(Transaction transaction) throws ResponseStatusException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(LedgerRequestType.SEND_TRANSACTION);
            oos.writeObject(transaction);
            byte[] bytes = bos.toByteArray();
            byte[] reply = serviceProxy.invokeOrdered(bytes);

            if (reply == null || reply[0] == 0x01) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

            this.logger.log(Level.INFO, "sendTransaction@Client: sent transaction");
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
            this.logger.log(Level.SEVERE, "error while retrieving ledger", e);
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

    @SuppressWarnings("unchecked")
    public List<Transaction> getExtract(Account account) {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut)) {

            objOut.writeObject(LedgerRequestType.GET_EXTRACT);
            objOut.writeObject(account);

            objOut.flush();
            byteOut.flush();

            byte[] reply = serviceProxy.invokeUnordered(byteOut.toByteArray());

            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                List<Transaction> extract = (List<Transaction>) objIn.readObject();
                //TODO - acc doesn't exist if there are no transactions
                if (extract == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

                return extract;
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "error while fetching extract for account: " + account.getAccountId(), e);
        }
        return null;
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
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut)) {

            objOut.writeObject(LedgerRequestType.GET_GLOBAL_LEDGER_VALUE);

            objOut.flush();
            byteOut.flush();

            byte[] reply = serviceProxy.invokeUnordered(byteOut.toByteArray());

            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                return objIn.readDouble();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error while calculating global value", e);
        }
        return -1.0;
    }
}
