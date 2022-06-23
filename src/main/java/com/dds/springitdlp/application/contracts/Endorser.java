package com.dds.springitdlp.application.contracts;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.LedgerHandlerConfig;
import com.dds.springitdlp.cryptography.Cryptography;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.naming.NoPermissionException;
import java.security.AccessControlException;
import java.security.Permissions;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class Endorser {

    private final Jail jail;
    private final Transaction mockTransaction;
    private final LedgerHandlerConfig ledgerHandlerConfig;
    private final Logger logger;

    @Autowired
    public Endorser(LedgerHandlerConfig ledgerHandlerConfig) {
        this.jail = new Jail(new Permissions());
        this.mockTransaction = new Transaction(Account.SYSTEM_ACC(), Account.SYSTEM_ACC(), 0.0);
        this.ledgerHandlerConfig = ledgerHandlerConfig;
        this.logger = Logger.getLogger(Endorser.class.getName());
    }

    public SmartContract endorse(SmartContract contract) throws NoPermissionException {
        try {
            // Enforce policies
            jail.toggle();

            contract.call(mockTransaction);

            contract.setUuid(UUID.randomUUID().toString());
            String signature = Cryptography.sign(contract.getUuid(), this.ledgerHandlerConfig.getPrivateKey());
            contract.setSignature(signature);

            this.logger.log(Level.INFO, "Endorsing smart contract");
            return contract;
        } catch (AccessControlException e) {
            this.logger.log(Level.WARNING, "Not endorsing smart contract, reason:");
            this.logger.log(Level.WARNING, "No permissions: " + e.getPermission());
            throw new NoPermissionException();
        } catch (OutOfMemoryError e) {
            this.logger.log(Level.WARNING, "Not endorsing smart contract, reason:");
            this.logger.log(Level.WARNING, "Too much memory");
            throw e;
        } finally {
            // Restore policies for normal functioning
            jail.toggle();
        }
    }
}
