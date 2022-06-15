package com.dds.springitdlp.application.bftSmart;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.ProposeResult;
import com.dds.springitdlp.application.entities.results.TransactionResult;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.LedgerHandler;
import com.dds.springitdlp.application.ledger.block.Block;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ConsensusServer extends DefaultSingleRecoverable implements CommandLineRunner {
    private final Logger logger;
    private final LedgerHandler ledgerHandler;

    @Autowired
    public ConsensusServer(LedgerHandler ledgerHandler) {
        this.ledgerHandler = ledgerHandler;
        this.logger = Logger.getLogger(ConsensusServer.class.getName());
    }

    @Override
    public void run(String... args) {
        int id = Integer.parseInt(System.getenv().get("REPLICA_ID"));
        new ServiceReplica(id, this, this);
    }

    @Override
    public byte[] getSnapshot() {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objOut = new ObjectOutputStream(byteOut)) {
            objOut.writeObject(this.ledgerHandler.getLedger());
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
            this.ledgerHandler.setLedger((Ledger) objIn.readObject());
        } catch (IOException | ClassNotFoundException e) {
            this.logger.log(Level.SEVERE, "error while installing snapshot", e);
        }
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext messageContext) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {

            LedgerRequestType reqType = (LedgerRequestType) objIn.readObject();
            switch (reqType) {
                case SEND_TRANSACTION -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        Transaction transaction = (Transaction) objIn.readObject();
                        TransactionResult result = this.ledgerHandler.sendTransaction(transaction);

                        oos.writeObject(result);
                        oos.flush();

                        this.logger.log(Level.INFO, "sendTransaction@Server: sending transaction result");
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
                case PROPOSE_BLOCK -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        Block block = (Block) objIn.readObject();
                        ProposeResult result = this.ledgerHandler.proposeBlock(block);

                        oos.writeObject(result);
                        oos.flush();

                        this.logger.log(Level.INFO, "sendTransaction@Server: sending transaction result");
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
                default -> {
                    return null;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext messageContext) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {

            LedgerRequestType reqType = (LedgerRequestType) objIn.readObject();
            switch (reqType) {
                case GET_LEDGER -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        Ledger ledger = this.ledgerHandler.getLedger();

                        oos.writeObject(ledger);
                        oos.flush();

                        this.logger.log(Level.INFO, "getLedger@Server: sending ledger");
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                case GET_BALANCE -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        Account account = (Account) objIn.readObject();
                        oos.writeDouble(this.ledgerHandler.getBalance(account));
                        oos.flush();

                        this.logger.log(Level.INFO, "getBalance@Server: sending balance for " + account.getAccountId());
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                case GET_EXTRACT -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        Account account = (Account) objIn.readObject();
                        oos.writeObject(this.ledgerHandler.getExtract(account));
                        oos.flush();

                        this.logger.log(Level.INFO, "getExtract@Server: fetching extract of " + account.getAccountId());
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                case GET_TOTAL_VALUE -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        List<Account> list = (List<Account>) objIn.readObject();
                        oos.writeDouble(this.ledgerHandler.getTotalValue(list));
                        oos.flush();

                        this.logger.log(Level.INFO, "getTotalValue@Server: sending total value of the given list");
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                case GET_GLOBAL_LEDGER_VALUE -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        oos.writeDouble(this.ledgerHandler.getGlobalLedgerValue());
                        oos.flush();

                        this.logger.log(Level.INFO, "getGlobalLedgerValue@Server");
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
}
