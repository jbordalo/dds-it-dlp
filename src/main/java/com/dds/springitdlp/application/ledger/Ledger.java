package com.dds.springitdlp.application.ledger;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.block.BlockHeader;
import com.dds.springitdlp.cryptography.Cryptography;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Getter
public class Ledger implements Serializable {
    @JsonIgnore
    private final List<Transaction> transactionPool;
    private final List<Block> blockchain;

    public Ledger() {
        this.transactionPool = new LinkedList<>();
        this.blockchain = new LinkedList<>();
    }

    /**
     * Auxiliary method. Gets balance but stops at given limit
     * This allows for better performance when checking if the account has enough
     * balance for a transaction, since we can stop iterating earlier
     * <p>
     * Iterating backwards is just a heuristic
     *
     * @param account - Account to check
     * @param limit   - limit balance needed
     * @return balance b, b might be greater than limit, due to the last transaction value sum
     */
    private double getLimitedBalance(Account account, double limit) {
        double balance = 0;

        Iterator<Block> blocks = ((LinkedList<Block>) this.blockchain).descendingIterator();

        while (blocks.hasNext()) {
            for (Transaction transaction : blocks.next().getTransactions()) {
                if (transaction.getOrigin().equals(account)) balance -= transaction.getAmount();
                else if (transaction.getDestination().equals(account)) balance += transaction.getAmount();
            }
            if (balance >= limit) break;
        }

        return balance;
    }

    public double getBalance(Account account) {
        return getLimitedBalance(account, Double.MAX_VALUE);
    }

    /**
     * Checks if transaction is already present in the ledger
     * @param transaction - Transaction to check
     * @return true if transaction is in the ledger, false otherwise
     */
    private boolean transactionInLedger(Transaction transaction) {
        Iterator<Block> blocks = ((LinkedList<Block>) this.blockchain).descendingIterator();

        while (blocks.hasNext()) {
            for (Transaction t : blocks.next().getTransactions()) {
                if (t.equals(transaction)) return true;
            }
        }
        return false;
    }

    /**
     * Applies a transaction to the ledger
     *
     * @param transaction - Transaction to be applied
     * @return TransactionResult
     */
    public TransactionResult sendTransaction(Transaction transaction) {
        if (transaction.getAmount() <= 0 || !Transaction.verify(transaction) ||
                this.getLimitedBalance(transaction.getOrigin(), transaction.getAmount()) < transaction.getAmount())
            return TransactionResult.FAILED_TRANSACTION;

        if (transactionPool.contains(transaction) || this.transactionInLedger(transaction)) return TransactionResult.REPEATED_TRANSACTION;

        transactionPool.add(transaction);

        return TransactionResult.OK_TRANSACTION;
    }

    public List<Transaction> getExtract(Account account) {
        List<Transaction> extract = new LinkedList<>();
        for (Block b : blockchain) {
            for (Transaction transaction : b.getTransactions()) {
                if (transaction.getOrigin().equals(account) || transaction.getDestination().equals(account))
                    extract.add(transaction);
            }
        }
        return extract;
    }

    @JsonIgnore
    public Block getBlock(Account account) {
        // if first block, mine the genesis block
        if (this.blockchain.size() == 0) {
            Transaction rewardTransaction = Transaction.REWARD_TRANSACTION(account);
            return Block.genesisBlock(rewardTransaction);
        }

        if (this.transactionPool.size() < Block.MIN_TRANSACTIONS_BLOCK - 1) return null;

        List<Transaction> transactions = this.transactionPool.subList(0, Block.MIN_TRANSACTIONS_BLOCK - 1);

        // this is the reward transaction
        transactions.add(Transaction.REWARD_TRANSACTION(account));

        Block lastBlock = this.blockchain.get(this.blockchain.size() - 1);

        return new Block(Cryptography.hash(lastBlock.toString()), BlockHeader.DEFAULT_DIFFICULTY, new ArrayList<>(transactions));
    }

    /**
     * Checks if the block is already in the chain by comparing previousHash
     * It compares previousHash because everything else is dependent on the reward transaction
     *
     * @param block - Block to check
     * @return true if the block is there, false otherwise
     */
    public boolean hasBlock(Block block) {
        Iterator<Block> blocks = ((LinkedList<Block>) this.blockchain).descendingIterator();

        while (blocks.hasNext()) {
            if (blocks.next().getHeader().getPreviousHash().equals(block.getHeader().getPreviousHash())) return true;
        }

        return false;
    }

    public void addBlock(Block block) {
        this.blockchain.add(block);

        for (Transaction transaction : block.getTransactions()) {
            this.transactionPool.remove(transaction);
        }
    }

    @JsonIgnore
    public double getGlobalValue() {
        double total = 0;

        for (Block b : blockchain) {
            for (Transaction transaction : b.getTransactions()) {
                // Only transfers from the system's account created value in the blockchain
                // These transactions are the mining rewards
                if (transaction.getOrigin().equals(Account.SYSTEM_ACC())) total += transaction.getAmount();
            }
        }

        return total;
    }
}
