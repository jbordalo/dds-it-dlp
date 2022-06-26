package com.dds.springitdlp.application.consensus.bftsmart;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.dds.springitdlp.application.consensus.ConsensusServer;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.LedgerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalOnProperty(name = "bftsmart.enabled")
@Component
public class BFTSMaRtServer extends DefaultSingleRecoverable implements CommandLineRunner {
    private final Logger logger = Logger.getLogger(BFTSMaRtServer.class.getName());
    private final LedgerHandler ledgerHandler;

    @Autowired
    public BFTSMaRtServer(LedgerHandler ledgerHandler) {
        this.ledgerHandler = ledgerHandler;
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
        return ConsensusServer.executeOrderedOperation(command, this.ledgerHandler, this.logger);
    }


    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext messageContext) {
        return ConsensusServer.executeUnorderedOperation(command, this.ledgerHandler, this.logger);
    }
}
