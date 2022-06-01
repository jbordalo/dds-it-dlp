package com.dds.springitdlp.application.bftSmart;

import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.core.messages.TOMMessageType;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.Ledger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ConsensusClient implements Consensus {
    AsynchServiceProxy serviceProxy;
    Logger logger;
    private static long TIMEOUT = 10;

    public ConsensusClient() {
        this.logger = Logger.getLogger(ConsensusClient.class.getName());
        int id = Integer.parseInt(System.getenv().get("REPLICA_ID"));
        this.serviceProxy = new AsynchServiceProxy(id);
    }

    @Override
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

    @Override
    public void sendAsyncTransaction(Transaction transaction) throws ResponseStatusException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(LedgerRequestType.SEND_TRANSACTION);
            oos.writeObject(transaction);
            byte[] bytes = bos.toByteArray();
            CompletableFuture<byte[]> future = new CompletableFuture<>();
            this.serviceProxy.invokeAsynchRequest(bytes, new ReplyHandler(serviceProxy, future), TOMMessageType.ORDERED_REQUEST);

            byte[] reply = future.get(TIMEOUT, TimeUnit.SECONDS);

            if (reply == null || reply[0] == 0x01) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

            this.logger.log(Level.INFO, "sendTransaction@Client: sent transaction");
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new ResponseStatusException(HttpStatus.REQUEST_TIMEOUT);
        }
    }

    @Override
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

    @Override
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
            this.logger.log(Level.SEVERE, "error while calculating balance", e);
        }
        return -1.0;
    }

    @SuppressWarnings("unchecked")
    @Override
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

                if (extract == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

                return extract;
            }
        } catch (IOException | ClassNotFoundException e) {
            this.logger.log(Level.SEVERE, "error while fetching extract for account: " + account.getAccountId(), e);
        }
        return null;
    }

    @Override
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
            this.logger.log(Level.SEVERE, "error while calculating balance", e);
        }
        return -1.0;
    }

    @Override
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
            this.logger.log(Level.SEVERE, "error while calculating global value", e);
        }
        return -1.0;
    }
}
