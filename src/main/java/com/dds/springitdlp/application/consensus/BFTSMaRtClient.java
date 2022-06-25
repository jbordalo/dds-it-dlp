package com.dds.springitdlp.application.consensus;

import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.core.messages.TOMMessageType;
import com.dds.springitdlp.application.contracts.SmartContract;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.ProposeResult;
import com.dds.springitdlp.application.entities.results.RegisterResult;
import com.dds.springitdlp.application.entities.results.TransactionResult;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.block.Block;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalOnProperty(name = "consensus.plane", havingValue = "bftsmart")
@Component
public class BFTSMaRtClient implements ConsensusPlane {
    AsynchServiceProxy serviceProxy;
    Logger logger;
    private final long TIMEOUT = 10;

    public BFTSMaRtClient() {
        this.logger = Logger.getLogger(BFTSMaRtClient.class.getName());
        int id = Integer.parseInt(System.getenv().get("REPLICA_ID"));
        this.serviceProxy = new AsynchServiceProxy(id);
    }

    @Override
    public TransactionResult sendTransaction(Transaction transaction) throws ResponseStatusException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(LedgerRequestType.SEND_TRANSACTION);
            oos.writeObject(transaction);
            byte[] bytes = bos.toByteArray();
            byte[] reply = serviceProxy.invokeOrdered(bytes);

            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                this.logger.log(Level.INFO, "sendTransaction@Client: sent transaction");
                return (TransactionResult) objIn.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<TransactionResult> sendAsyncTransaction(Transaction transaction) throws ResponseStatusException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(LedgerRequestType.SEND_ASYNC_TRANSACTION);
            oos.writeObject(transaction);
            byte[] bytes = bos.toByteArray();
            CompletableFuture<List<TransactionResult>> future = new CompletableFuture<>();
            this.serviceProxy.invokeAsynchRequest(bytes, new BFTReplyHandler(serviceProxy, future), TOMMessageType.ORDERED_REQUEST);

            List<TransactionResult> reply = future.get(TIMEOUT, TimeUnit.SECONDS);

            this.logger.log(Level.INFO, "sendTransaction@Client: sent transaction");
            return reply;
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

            byte[] reply = this.serviceProxy.invokeUnordered(byteOut.toByteArray());

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

    @Override
    public ProposeResult proposeBlock(Block block) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(LedgerRequestType.PROPOSE_BLOCK);
            oos.writeObject(block);

            byte[] bytes = bos.toByteArray();
            byte[] reply = this.serviceProxy.invokeOrdered(bytes);

            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                this.logger.log(Level.INFO, "proposeBlock@Client");
                return (ProposeResult) objIn.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RegisterResult registerSmartContract(SmartContract contract) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(LedgerRequestType.REGISTER_SMART_CONTRACT);
            oos.writeObject(contract);

            byte[] bytes = bos.toByteArray();
            byte[] reply = this.serviceProxy.invokeOrdered(bytes);

            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {
                this.logger.log(Level.INFO, "registerSmartContract@Client");
                return (RegisterResult) objIn.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return RegisterResult.CONTRACT_REJECTED;
    }
}
