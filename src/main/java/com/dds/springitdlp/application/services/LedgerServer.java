package com.dds.springitdlp.application.services;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.dds.springitdlp.application.entities.Transaction;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LedgerServer extends DefaultSingleRecoverable {
    private final Logger logger;

    public LedgerServer() {
        logger = Logger.getLogger(LedgerServer.class.getName());
        int id = Integer.parseInt(System.getenv().get("REPLICA_ID"));
        new ServiceReplica(id, this, this);
    }

    public static void main(String[] args) {
        new LedgerServer();
    }

    @Override
    public void installSnapshot(byte[] bytes) {
    }

    @Override
    public byte[] getSnapshot() {
        return new byte[0];
    }

    @Override
    public byte[] appExecuteOrdered(byte[] bytes, MessageContext messageContext) {
        return new byte[0];
    }

    @Override
    public byte[] appExecuteUnordered(byte[] bytes, MessageContext messageContext) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInputStream ois = new ObjectInputStream(bis)) {
            Transaction transaction = (Transaction) ois.readObject();
            System.out.println(transaction);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return "transaction completed".getBytes(StandardCharsets.UTF_8);
    }
}
