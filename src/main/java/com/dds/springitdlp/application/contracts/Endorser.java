package com.dds.springitdlp.application.contracts;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.LedgerHandlerConfig;
import com.dds.springitdlp.cryptography.Cryptography;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.AccessControlException;
import java.security.Permissions;

@Component
public class Endorser {

    private final Transaction mockTransaction;
    private final Jail jail;

    private final LedgerHandlerConfig ledgerHandlerConfig;

    @Autowired
    public Endorser(LedgerHandlerConfig ledgerHandlerConfig) {
        this.ledgerHandlerConfig = ledgerHandlerConfig;
        this.mockTransaction = new Transaction(Account.SYSTEM_ACC(), Account.SYSTEM_ACC(), 0.0);
        this.jail = new Jail(new Permissions());
    }

    public SmartContract endorse(SmartContract contract) {
        try {
            System.out.println("Running with confinement:");

            jail.toggle();

            contract.call(mockTransaction);

            // TODO better signature
            contract.setSignature(Cryptography.sign(contract.toString(), this.ledgerHandlerConfig.getPrivateKey()));

            jail.toggle();

            return contract;
        } catch (AccessControlException e) {
            System.out.println("Not endorsing smart contract");
            return null;
        }
    }
}
