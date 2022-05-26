package com.dds.springitdlp.application.entities;

import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Ledger implements Serializable {
    @Getter
    private final Map<Account, List<Transaction>> map;

    public Ledger() {
        this.map = new HashMap<>();
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

        // TODO
        if (!Transaction.verify(transaction)) return false;

        if (transaction.getAmount() <= 0) return false;

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
