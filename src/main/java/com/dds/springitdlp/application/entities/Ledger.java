package com.dds.springitdlp.application.entities;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class Ledger implements Serializable {
    @Getter
    private final Map<Account, List<Transaction>> map;

    public Ledger() {
        map = new HashMap<>();
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

    public int sendTransaction(Transaction transaction) {
        if (transaction.getAmount() <= 0) return 1;

        Account origin = transaction.getOrigin();
        Account destination = transaction.getDestination();

        List<Transaction> originList = this.map.get(origin);
        if (originList == null) {
            originList = new LinkedList<>();
            originList.add(Transaction.SYS_INIT(origin));
            this.map.put(origin, originList);
        }

        if (this.getBalance(origin) < transaction.getAmount()) return 1;

        List<Transaction> destinationList = this.map.get(destination);
        if (destinationList == null) {
            destinationList = new LinkedList<>();
            destinationList.add(Transaction.SYS_INIT(destination));
            this.map.put(destination, destinationList);
        }
        destinationList.add(transaction);

        originList.add(new Transaction(origin, destination, -transaction.getAmount()));

        return 0;
    }
}
