package com.dds.springitdlp.application.bftSmart;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.dds.springitdlp.application.entities.Ledger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class LedgerServer extends DefaultSingleRecoverable implements CommandLineRunner {
    private final Logger logger;
    private final LedgerHandler ledgerHandler;

    public LedgerServer() throws IOException {
        this.logger = Logger.getLogger(LedgerServer.class.getName());
        this.ledgerHandler = new LedgerHandler();
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
                    return this.ledgerHandler.sendTransaction(objIn);
                }
                default -> {
                    return null;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext messageContext) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {

            LedgerRequestType reqType = (LedgerRequestType) objIn.readObject();
            switch (reqType) {
                case GET_LEDGER -> {
                    return this.ledgerHandler.getLedger();
                }
                case GET_BALANCE -> {
                    return this.ledgerHandler.getBalance(objIn);
                }
                case GET_EXTRACT -> {
                    return this.ledgerHandler.getExtract(objIn);
                }
                case GET_TOTAL_VALUE -> {
                    return this.ledgerHandler.getTotalValue(objIn);
                }
                case GET_GLOBAL_LEDGER_VALUE -> {
                    return this.ledgerHandler.getGlobalLedgerValue();
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
