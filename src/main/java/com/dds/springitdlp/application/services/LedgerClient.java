package com.dds.springitdlp.application.services;

import bftsmart.tom.ServiceProxy;
import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
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

    public void sendTransaction(Transaction transaction) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(transaction);
            byte[] bts = bos.toByteArray();
            bts = serviceProxy.invokeOrdered(bts);
            //TODO
            String s = new String(bts, StandardCharsets.UTF_8);
            logger.log(Level.INFO, s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Ledger getLedger() {
        byte[] bytes = serviceProxy.invokeUnordered(null);
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {
            return (Ledger) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "error while retrieving ledger", e);
        }
        return null;
    }
}
