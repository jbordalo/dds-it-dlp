package com.dds.springitdlp.application.consensus;

import com.dds.springitdlp.application.contracts.SmartContract;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.LedgerHandler;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.results.ProposeResult;
import com.dds.springitdlp.application.results.RegisterResult;
import com.dds.springitdlp.application.results.TransactionResult;

import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsensusServer {

    public static byte[] executeOrderedOperation(byte[] command, LedgerHandler ledgerHandler, Logger logger) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {

            LedgerRequestType reqType = (LedgerRequestType) objIn.readObject();
            switch (reqType) {
                case SEND_TRANSACTION, SEND_ASYNC_TRANSACTION -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        Transaction transaction = (Transaction) objIn.readObject();
                        boolean signed = reqType == LedgerRequestType.SEND_ASYNC_TRANSACTION;
                        TransactionResult result = ledgerHandler.sendTransaction(transaction, signed);

                        oos.writeObject(result);
                        oos.flush();

                        logger.log(Level.INFO, "sendTransaction@Server: sending transaction result");
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
                case PROPOSE_BLOCK -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        Block block = (Block) objIn.readObject();
                        ProposeResult result = ledgerHandler.proposeBlock(block);

                        oos.writeObject(result);
                        oos.flush();

                        logger.log(Level.INFO, "sendTransaction@Server: sending transaction result");
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
                case REGISTER_SMART_CONTRACT -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        SmartContract contract = (SmartContract) objIn.readObject();
                        RegisterResult result = ledgerHandler.registerSmartContract(contract);

                        oos.writeObject(result);
                        oos.flush();

                        logger.log(Level.INFO, "registerSmartContract@Server: registering smart contract");
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
    public static byte[] executeUnorderedOperation(byte[] command, LedgerHandler ledgerHandler, Logger logger) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {

            LedgerRequestType reqType = (LedgerRequestType) objIn.readObject();
            switch (reqType) {
                case GET_LEDGER -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        Ledger ledger = ledgerHandler.getLedger();

                        oos.writeObject(ledger);
                        oos.flush();

                        logger.log(Level.INFO, "getLedger@Server: sending ledger");
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                case GET_BALANCE -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        Account account = (Account) objIn.readObject();
                        oos.writeDouble(ledgerHandler.getBalance(account));
                        oos.flush();

                        logger.log(Level.INFO, "getBalance@Server: sending balance for " + account.getAccountId());
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
                        oos.writeObject(ledgerHandler.getExtract(account));
                        oos.flush();

                        logger.log(Level.INFO, "getExtract@Server: fetching extract of " + account.getAccountId());
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
                        oos.writeDouble(ledgerHandler.getTotalValue(list));
                        oos.flush();

                        logger.log(Level.INFO, "getTotalValue@Server: sending total value of the given list");
                        return bos.toByteArray();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                case GET_GLOBAL_LEDGER_VALUE -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        oos.writeDouble(ledgerHandler.getGlobalLedgerValue());
                        oos.flush();

                        logger.log(Level.INFO, "getGlobalLedgerValue@Server");
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
