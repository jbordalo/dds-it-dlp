package com.dds.springitdlp.application.ledger;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.block.Block;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@RedisHash("Ledger")
@Getter
public class Ledger implements Serializable {
    @SuppressWarnings("FieldMayBeFinal")
    private List<Block> blockchain;

    @JsonIgnore
    @Id
    private String id = System.getenv("REPLICA_ID");

    public Ledger() {
        this.blockchain = new LinkedList<>();
    }

    /**
     * Auxiliary method. Gets balance but stops at given limit
     * This allows for better performance when checking if the account has enough
     * balance for a transaction, since we can stop iterating earlier
     *
     * @param account - Account to check
     * @param limit   - limit balance needed
     * @return balance b, b might be greater than limit, due to the last transaction value sum
     */
    private double getLimitedBalance(Account account, double limit) {
        double balance = 0;

        Iterator<Block> blocks = this.blockchain.iterator();

        while (blocks.hasNext()) {
            for (Transaction transaction : blocks.next().getTransactions()) {
                if (transaction.getOrigin().equals(account)) balance -= transaction.getAmount();
                else if (transaction.getDestination().equals(account)) balance += transaction.getAmount();
            }
            if (balance >= limit) break;
        }

        return balance;
    }

    public boolean hasBalance(Account account, double balance) {
        return this.getLimitedBalance(account, balance) >= balance;
    }

    public double getBalance(Account account) {
        return getLimitedBalance(account, Double.MAX_VALUE);
    }

    /**
     * Checks if transaction is already present in the ledger
     *
     * @param transaction - Transaction to check
     * @return true if transaction is in the ledger, false otherwise
     */
    public boolean transactionInLedger(Transaction transaction) {
        Iterator<Block> blocks = this.blockchain.iterator();

        while (blocks.hasNext()) {
            for (Transaction t : blocks.next().getTransactions()) {
                if (t.equals(transaction)) return true;
            }
        }
        return false;
    }

    public List<Transaction> getExtract(Account account) {
        List<Transaction> extract = new LinkedList<>();
        for (Block b : this.blockchain) {
            for (Transaction transaction : b.getTransactions()) {
                if (transaction.getOrigin().equals(account) || transaction.getDestination().equals(account))
                    extract.add(transaction);
            }
        }
        return extract;
    }

    /**
     * Checks if the block is already in the chain by comparing previousHash
     * It compares previousHash because everything else is dependent on the reward transaction
     *
     * @param block - Block to check
     * @return true if the block is there, false otherwise
     */
    public boolean hasBlock(Block block) {
        for (Block value : this.blockchain) {
            if (value.getHeader().getPreviousHash().equals(block.getHeader().getPreviousHash())) return true;
        }

        return false;
    }

    public void addBlock(Block block) {
        this.blockchain.add(block);
    }

    @JsonIgnore
    public Block getLastBlock() {
        if (this.blockchain.isEmpty()) return null;
        else return this.blockchain.get(this.blockchain.size() - 1);
    }

    @JsonIgnore
    public double getGlobalValue() {
        double total = 0;

        for (Block b : this.blockchain) {
            for (Transaction transaction : b.getTransactions()) {
                // Only transfers from the system's account created value in the blockchain
                // These transactions are the mining rewards
                if (transaction.getOrigin().equals(Account.SYSTEM_ACC())) total += transaction.getAmount();
            }
        }

        return total;
    }
}
