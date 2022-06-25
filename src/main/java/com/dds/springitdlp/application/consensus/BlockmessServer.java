package com.dds.springitdlp.application.consensus;

import applicationInterface.ApplicationInterface;
import com.dds.springitdlp.application.contracts.SmartContract;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.ProposeResult;
import com.dds.springitdlp.application.entities.results.RegisterResult;
import com.dds.springitdlp.application.entities.results.TransactionResult;
import com.dds.springitdlp.application.ledger.LedgerHandler;
import com.dds.springitdlp.application.ledger.block.Block;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalOnProperty(name = "blockmess.enabled")
@Component
public class BlockmessServer extends ApplicationInterface {

    private final Logger logger = Logger.getLogger(BlockmessServer.class.getName());
    private final LedgerHandler ledgerHandler;

    @Autowired
    public BlockmessServer(LedgerHandler ledgerHandler) {
        super(new String[]{"port=" + System.getenv("BLOCKMESS_PORT")});
        this.ledgerHandler = ledgerHandler;
    }

    @Override
    public byte[] processOperation(byte[] command) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {

            LedgerRequestType reqType = (LedgerRequestType) objIn.readObject();
            switch (reqType) {
                case SEND_TRANSACTION, SEND_ASYNC_TRANSACTION -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        Transaction transaction = (Transaction) objIn.readObject();
                        boolean signed = reqType == LedgerRequestType.SEND_ASYNC_TRANSACTION;
                        TransactionResult result = this.ledgerHandler.sendTransaction(transaction, signed);

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
                case REGISTER_SMART_CONTRACT -> {
                    try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        SmartContract contract = (SmartContract) objIn.readObject();
                        RegisterResult result = this.ledgerHandler.registerSmartContract(contract);

                        oos.writeObject(result);
                        oos.flush();

                        this.logger.log(Level.INFO, "registerSmartContract@Server: registering smart contract");
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
}
