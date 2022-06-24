package com.dds.springitdlp.application.ledger;

import com.dds.springitdlp.application.contracts.SmartContract;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.ProposeResult;
import com.dds.springitdlp.application.entities.results.RegisterResult;
import com.dds.springitdlp.application.entities.results.TransactionResult;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.block.BlockHeader;
import com.dds.springitdlp.cryptography.Cryptography;
import com.dds.springitdlp.dataPlane.DataPlane;
import com.dds.springitdlp.dataPlane.SmartContractRegistry;
import com.dds.springitdlp.dataPlane.TransactionPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalOnProperty(name = "endorser", havingValue = "false")
@Component
public class LedgerHandler {
    private final ServerKeys config;
    private final Logger logger;
    private final DataPlane dataPlane;

    @Autowired
    public LedgerHandler(ServerKeys config, DataPlane dataPlane) {
        this.dataPlane = dataPlane;
        this.config = config;
        this.logger = Logger.getLogger(LedgerHandler.class.getName());
    }

    /**
     * Wrapper function for the Ledger sendTransaction,
     * persists data if there are no errors.
     *
     * @param transaction - Transaction to be handled
     * @param signed      - Indicates if result should be signed
     * @return true if there was an error, false otherwise
     */
    public TransactionResult sendTransaction(Transaction transaction, boolean signed) {
        this.logger.log(Level.INFO, "sendTransaction@Server: " + transaction.toString());

        // TODO where check
        if (transaction.getSmartContractUuid() != null) {
            SmartContract smartContract = this.dataPlane.readSmartContractRegistry().getSmartContract(transaction.getSmartContractUuid());

            if (smartContract == null) {
                this.logger.log(Level.WARNING, "sendTransaction@Server: Didn't find provided smart contract");
                return this.getResult(TransactionResultStatus.FAILED_TRANSACTION, signed);
            }

            this.logger.log(Level.INFO, "sendTransaction@Server: Running provided smart contract");
            TransactionResultStatus smartContractResult = smartContract.call(transaction);
            if (smartContractResult != TransactionResultStatus.OK_TRANSACTION) {
                this.logger.log(Level.INFO, "sendTransaction@Server: Transaction not accepted by smart contract");
                return this.getResult(smartContractResult, signed);
            }
            this.logger.log(Level.INFO, "sendTransaction@Server: Transaction accepted by smart contract");
        }

        if (transaction.getAmount() <= 0 || !Transaction.verify(transaction) ||
                !this.getLedger().hasBalance(transaction.getOrigin(), transaction.getAmount())) {
            return this.getResult(TransactionResultStatus.FAILED_TRANSACTION, signed);
        }

        TransactionPool transactionPool = this.dataPlane.readTransactionPool();
        List<Transaction> pool = transactionPool.getTransactionPool();

        if (pool.contains(transaction) || this.getLedger().transactionInLedger(transaction)) {
            return this.getResult(TransactionResultStatus.REPEATED_TRANSACTION, signed);
        }

        pool.add(transaction);

        this.dataPlane.writeTransactionPool(transactionPool);

        return this.getResult(TransactionResultStatus.OK_TRANSACTION, signed);
    }

    private TransactionResult getResult(TransactionResultStatus result, boolean signed) {
        if (!signed) return new TransactionResult(result);
        return new TransactionResult(result, System.getenv("REPLICA_ID"), Cryptography.sign(result.toString(), this.config.getPrivateKey()));
    }

    public Ledger getLedger() {
        return this.dataPlane.readLedger();
    }

    public double getBalance(Account account) {
        return this.getLedger().getBalance(account);
    }

    public double getGlobalLedgerValue() {
        return this.getLedger().getGlobalValue();
    }

    public List<Transaction> getExtract(Account account) {
        return this.getLedger().getExtract(account);
    }

    public double getTotalValue(List<Account> list) {
        double total = 0.0;

        for (Account a : list) {
            total += this.getLedger().getBalance(a);
        }

        return total;
    }

    public void setLedger(Ledger ledger) {
        this.dataPlane.writeLedger(ledger);
    }

    public Block getBlock(Account account) {

        Block lastBlock = this.getLedger().getLastBlock();

        // If blockchain is empty we'll mine the genesis block
        if (lastBlock == null) {
            Transaction rewardTransaction = Transaction.REWARD_TRANSACTION(account);
            return Block.genesisBlock(rewardTransaction);
        }

        TransactionPool transactionPool = this.dataPlane.readTransactionPool();
        List<Transaction> pool = transactionPool.getTransactionPool();

        if (pool.size() < Block.MIN_TRANSACTIONS_BLOCK - 1) return null;

        List<Transaction> transactions = pool.subList(0, Block.MIN_TRANSACTIONS_BLOCK - 1);

        // this is the reward transaction
        transactions.add(Transaction.REWARD_TRANSACTION(account));

        return new Block(Cryptography.hash(lastBlock.toString()), BlockHeader.DEFAULT_DIFFICULTY, new ArrayList<>(transactions));
    }

    public ProposeResult proposeBlock(Block block) {
        if (Block.checkBlock(block) && !this.hasBlock(block)) {
            Ledger ledger = this.getLedger();
            ledger.addBlock(block);

            this.dataPlane.writeLedger(ledger);

            TransactionPool transactionPool = this.dataPlane.readTransactionPool();
            List<Transaction> pool = transactionPool.getTransactionPool();

            for (Transaction transaction : block.getTransactions()) {
                pool.remove(transaction);
            }

            this.dataPlane.writeTransactionPool(transactionPool);

            return ProposeResult.BLOCK_ACCEPTED;
        }
        return ProposeResult.BLOCK_REJECTED;
    }

    public boolean hasBlock(Block block) {
        return this.getLedger().hasBlock(block);
    }

    public RegisterResult registerSmartContract(SmartContract contract) {
        if (!Cryptography.verify(this.config.getEndorserKey(contract.getEndorserId()), contract.serialize(), contract.getSignature()))
            return RegisterResult.CONTRACT_REJECTED;

        SmartContractRegistry contracts = this.dataPlane.readSmartContractRegistry();
        contracts.registerContract(contract);
        this.dataPlane.writeSmartContractRegistry(contracts);

        return RegisterResult.CONTRACT_REGISTERED;
    }
}