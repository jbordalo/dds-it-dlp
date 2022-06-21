package com.dds.springitdlp.application.ledger;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.ProposeResult;
import com.dds.springitdlp.application.entities.results.TransactionResult;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.block.BlockHeader;
import com.dds.springitdlp.cryptography.Cryptography;
import com.dds.springitdlp.dataPlane.DataPlane;
import com.dds.springitdlp.dataPlane.TransactionPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class LedgerHandler {
    private final LedgerHandlerConfig config;
    private final Logger logger;


    private final DataPlane dataPlane;

    @Autowired
    public LedgerHandler(LedgerHandlerConfig config, DataPlane dataPlane) {
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
        TransactionResult result = new TransactionResult();

        if (transaction.getAmount() <= 0 || !Transaction.verify(transaction) ||
                !this.getLedger().hasBalance(transaction.getOrigin(), transaction.getAmount())) {
            result.setResult(TransactionResultStatus.FAILED_TRANSACTION);
            return result;
        }

        // TODO exception handling
        TransactionPool transactionPool = this.dataPlane.readTransactionPool();
        List<Transaction> pool = transactionPool.getTransactionPool();

        if (pool.contains(transaction) || this.getLedger().transactionInLedger(transaction)) {
            result.setResult(TransactionResultStatus.REPEATED_TRANSACTION);
            return result;
        }

        pool.add(transaction);

        this.dataPlane.writeTransactionPool(transactionPool);

        result.setResult(TransactionResultStatus.OK_TRANSACTION);

        if (signed) {
            result.setSignature(Cryptography.sign(result.getResult().toString(), this.config.getPrivateKey()));
            result.setReplicaId(System.getenv("REPLICA_ID"));
        }
        return result;
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
}