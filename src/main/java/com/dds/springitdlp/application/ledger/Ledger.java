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
import java.util.LinkedList;
import java.util.List;

@Getter
public class Ledger implements Serializable {
    private final List<Transaction> transactionPool;
    private final List<Block> blockchain;

    public Ledger() {
        this.transactionPool = new LinkedList<>();
        this.blockchain = new LinkedList<>();
    }

    public double getBalance(Account account) {
        double balance = 0;

        for (Block b : blockchain) {
            for (Transaction transaction : b.getTransactions()) {
                if (transaction.getOrigin().equals(account)) balance -= transaction.getAmount();
                else if (transaction.getDestination().equals(account)) balance += transaction.getAmount();
            }
        }

        return balance;
    }

    /**
     * Applies a transaction to the ledger
     *
     * @param transaction Transaction to be applied
     * @return boolean true if transaction went through, false otherwise
     */
    public boolean sendTransaction(Transaction transaction) {
        if (!Transaction.verify(transaction) || transaction.getAmount() <= 0) return false;

        if (this.getBalance(transaction.getOrigin()) < transaction.getAmount()) return false;

        // TODO address double spending by checking on the blockchain too
        if (!transactionPool.contains(transaction))
            transactionPool.add(transaction);

        return true;
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

    public boolean hasBlock(Block block) {
        return this.blockchain.contains(block);
    }

    public void addBlock(Block block) {
        this.blockchain.add(block);

        for (Transaction transaction : block.getTransactions()) {
            this.transactionPool.remove(transaction);
        }
    }

    public double getGlobalValue() {
        double total = 0;

        for (Block b : blockchain) {
            for (Transaction transaction : b.getTransactions()) {
                if (transaction.getOrigin().equals(Account.SYSTEM_ACC())) total += transaction.getAmount();
            }
        }

        return total;
    }
}
