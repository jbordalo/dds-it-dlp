package com.dds.springitdlp.dataPlane.redis;

import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.dataPlane.DataPlane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "persistence.plane", havingValue = "redis")
public class DataPlaneRedis implements DataPlane {
    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;
    private final SmartContractRegistryRepository smartContractRegistryRepository;

    @Autowired
    public DataPlaneRedis(LedgerRepository ledgerRepository, TransactionRepository transactionRepository, SmartContractRegistryRepository smartContractRegistryRepository) {
        this.ledgerRepository = ledgerRepository;
        this.transactionRepository = transactionRepository;
        this.smartContractRegistryRepository = smartContractRegistryRepository;
        this.initialize();
    }

    private void initialize() {
        Optional<Ledger> ledger = this.ledgerRepository.findById(System.getenv("REPLICA_ID"));

        if (ledger.isEmpty()) this.ledgerRepository.save(new Ledger());

        Optional<TransactionPool> transactionPool = this.transactionRepository.findById(System.getenv("REPLICA_ID"));

        if (transactionPool.isEmpty()) this.transactionRepository.save(new TransactionPool());

        Optional<SmartContractRegistry> smartContractRegistry = this.smartContractRegistryRepository.findById(System.getenv("REPLICA_ID"));

        if (smartContractRegistry.isEmpty()) this.smartContractRegistryRepository.save(new SmartContractRegistry());
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

    @Override
    public SmartContractRegistry readSmartContractRegistry() {
        return this.smartContractRegistryRepository.findById(System.getenv("REPLICA_ID")).get();
    }

    @Override
    public void writeSmartContractRegistry(SmartContractRegistry smartContractRegistry) {
        this.smartContractRegistryRepository.save(smartContractRegistry);
    }
}
