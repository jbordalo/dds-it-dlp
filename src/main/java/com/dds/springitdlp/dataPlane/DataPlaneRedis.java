package com.dds.springitdlp.dataPlane;

import com.dds.springitdlp.application.ledger.Ledger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "persistence.plane", havingValue = "redis")
public class DataPlaneRedis implements DataPlane {

    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public DataPlaneRedis(LedgerRepository ledgerRepository, TransactionRepository transactionRepository) {
        this.ledgerRepository = ledgerRepository;
        this.transactionRepository = transactionRepository;
        this.initialize();
    }

    private void initialize() {
        Optional<Ledger> ledger = this.ledgerRepository.findById(System.getenv("REPLICA_ID"));

        if (ledger.isEmpty()) this.ledgerRepository.save(new Ledger());

        Optional<TransactionPool> transactionPool = this.transactionRepository.findById(System.getenv("REPLICA_ID"));

        if (transactionPool.isEmpty()) this.transactionRepository.save(new TransactionPool());
    }

    @Override
    public Ledger readLedger() {
        return this.ledgerRepository.findById(System.getenv("REPLICA_ID")).get();
    }

    @Override
    public void writeLedger(Ledger ledger) {
        this.ledgerRepository.save(ledger);
    }

    @Override
    public TransactionPool readTransactionPool() {
        return this.transactionRepository.findById(System.getenv("REPLICA_ID")).get();
    }

    @Override
    public void writeTransactionPool(TransactionPool transactionPool) {
        this.transactionRepository.save(transactionPool);
    }
}
