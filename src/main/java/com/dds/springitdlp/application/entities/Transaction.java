package com.dds.springitdlp.application.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction implements Serializable {

    public static final double INITIAL_VALUE = 100.0;

    private Account origin;
    private Account destination;
    private double amount;

    private int nonce;
    private long timestamp;

    public static Transaction SYS_INIT(Account account) {
        return new Transaction(Account.SYSTEM_ACC(), account, Transaction.INITIAL_VALUE, 0, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return nonce == that.nonce && origin.equals(that.origin) && destination.equals(that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, destination, nonce);
    }
}
