package com.dds.springitdlp.dataPlane;

import com.dds.springitdlp.application.ledger.Ledger;

public interface DataPlane {
    Ledger readLedger();

    void writeLedger(Ledger ledger);

    TransactionPool readTransactionPool();

    void writeTransactionPool(TransactionPool transactionPool);
}
