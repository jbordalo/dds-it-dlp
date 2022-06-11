package com.dds.springitdlp.application.ledger;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.merkleTree.MerkleTree;
import lombok.Getter;

import java.io.Serializable;
import java.util.*;

@Getter
public class Ledger implements Serializable {
    private final Map<Account, List<Transaction>> map;
    private final List<Transaction> transactionPool;
    private final List<Block> blockchain;

    public Ledger() {
        this.map = new HashMap<>();
        this.transactionPool = new LinkedList<>();
        this.blockchain = new LinkedList<>();
        this.blockchain.add(Block.genesisBlock());
    }

    public double getBalance(Account account) {
        double balance = 0;
        List<Transaction> transactions = this.map.get(account);

        if (transactions == null) return -1.0;

        for (Transaction transaction : transactions) {
            balance += transaction.getAmount();
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

        if (!transactionPool.contains(transaction))
            transactionPool.add(transaction);

        Account origin = transaction.getOrigin();
        Account destination = transaction.getDestination();
        int nonce = transaction.getNonce();
        long timestamp = transaction.getTimestamp();
        String signature = transaction.getSignature();

        List<Transaction> originList = this.map.get(origin);
        if (originList == null) {
            originList = new LinkedList<>();
            originList.add(Transaction.SYS_INIT(origin));
            this.map.put(origin, originList);
        } else {
            if (originList.contains(transaction)) {
                System.out.println("Repeated transaction");
                return true;
            }
        }

        if (this.getBalance(origin) < transaction.getAmount()) return false;

        List<Transaction> destinationList = this.map.get(destination);
        if (destinationList == null) {
            destinationList = new LinkedList<>();
            destinationList.add(Transaction.SYS_INIT(destination));
            this.map.put(destination, destinationList);
        }

        destinationList.add(transaction);

        originList.add(new Transaction(origin, destination, -transaction.getAmount(), nonce, timestamp, signature));
        return true;
    }

    public List<Transaction> getExtract(Account account) {
        return this.map.get(account);
    }

    public Block getBlock() {
        if (this.transactionPool.size() < Block.MIN_TRANSACTIONS_BLOCK) return null;

        // TODO remove from transaction pool (after it is confirmed as a block)
        List<Transaction> transactions = this.transactionPool.subList(0, Block.MIN_TRANSACTIONS_BLOCK);

        Block lastBlock = this.blockchain.get(this.blockchain.size() - 1);
        // TODO move hash to a better place and maybe hash the bytes instead of the string
        return new Block(MerkleTree.hash(lastBlock.toString()), 0, new ArrayList<>(transactions));
    }
}
