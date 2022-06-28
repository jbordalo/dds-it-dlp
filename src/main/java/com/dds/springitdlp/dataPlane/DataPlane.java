package com.dds.springitdlp.dataPlane;

import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.dataPlane.redis.SmartContractRegistry;
import com.dds.springitdlp.dataPlane.redis.TransactionPool;

public interface DataPlane {
    Ledger readLedger();

    void writeLedger(Ledger ledger);

    TransactionPool readTransactionPool();

    void writeTransactionPool(TransactionPool transactionPool);

    SmartContractRegistry readSmartContractRegistry();

    void writeSmartContractRegistry(SmartContractRegistry smartContractRegistry);
}
