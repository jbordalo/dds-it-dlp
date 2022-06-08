package com.dds.springitdlp.application.ledger;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Ledger implements Serializable {
    @Getter
    private final Map<Account, List<Transaction>> map;
    private final List<Transaction> transactionPool;

    public Ledger() {
        this.map = new HashMap<>();
        this.transactionPool = new LinkedList<>();
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
}
