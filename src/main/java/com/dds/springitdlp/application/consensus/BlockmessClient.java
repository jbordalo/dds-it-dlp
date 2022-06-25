package com.dds.springitdlp.application.consensus;

import applicationInterface.ApplicationInterface;
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
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
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

@ConditionalOnProperty(name = "consensus.plane", havingValue = "blockmess")
@Component
public class BlockmessClient implements ConsensusPlane {
    ApplicationInterface applicationInterface;
    Logger logger;
    private final long TIMEOUT = 10;

    @Autowired
    public BlockmessClient(ApplicationInterface applicationInterface) {
        this.logger = Logger.getLogger(BlockmessClient.class.getName());
        this.applicationInterface = applicationInterface;
    }

    @Override
    public TransactionResult sendTransaction(Transaction transaction) throws ResponseStatusException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(LedgerRequestType.SEND_TRANSACTION);
            oos.writeObject(transaction);
            byte[] bytes = bos.toByteArray();
            byte[] reply = applicationInterface.invokeSyncOperation(bytes).getLeft();

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

            this.applicationInterface.invokeAsyncOperation(bytes, new BlockmessReplyHandler(future));

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
        throw new NotImplementedException();
    }

    @Override
    public double getBalance(Account account) throws ResponseStatusException {
        throw new NotImplementedException();
    }

    @Override
    public List<Transaction> getExtract(Account account) {
        throw new NotImplementedException();
    }

    @Override
    public double getTotalValue(List<Account> accounts) {
        throw new NotImplementedException();
    }

    @Override
    public double getGlobalLedgerValue() {
        throw new NotImplementedException();
    }

    @Override
    public ProposeResult proposeBlock(Block block) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(LedgerRequestType.PROPOSE_BLOCK);
            oos.writeObject(block);

            byte[] bytes = bos.toByteArray();
            byte[] reply = this.applicationInterface.invokeSyncOperation(bytes).getLeft();

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
            byte[] reply = this.applicationInterface.invokeSyncOperation(bytes).getLeft();

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
