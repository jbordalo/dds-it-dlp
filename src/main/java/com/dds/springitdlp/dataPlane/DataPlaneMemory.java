package com.dds.springitdlp.dataPlane;

import com.dds.springitdlp.application.ledger.Ledger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "persistence.plane", havingValue = "memory")
public class DataPlaneMemory implements DataPlane {

    private Ledger ledger = new Ledger();
    private TransactionPool transactionPool = new TransactionPool();

    @Override
    public Ledger readLedger() {
        return this.ledger;
    }

    @Override
    public void writeLedger(Ledger ledger) {
        this.ledger = ledger;
    }

    @Override
    public TransactionPool readTransactionPool() {
        return this.transactionPool;
    }

    @Override
    public void writeTransactionPool(TransactionPool transactionPool) {
        this.transactionPool = transactionPool;
    }
}
