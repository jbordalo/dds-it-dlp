package com.dds.springitdlp.application.entities;

import com.dds.springitdlp.cryptography.Cryptography;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction implements Serializable {

    public static final double MINING_REWARD = 100.0;

    private Account origin;
    private Account destination;
    private double amount;
    private int nonce;
    private long timestamp;

    private String signature;

    /**
     * Special transaction for the reward miners get
     *
     * @param account - account that gets the reward
     * @return reward Transaction
     */
    public static Transaction REWARD_TRANSACTION(Account account) {
        return new Transaction(Account.SYSTEM_ACC(), account, Transaction.MINING_REWARD * 10, 0, System.currentTimeMillis(), null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return nonce == that.nonce && origin.equals(that.origin) && destination.equals(that.destination);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "origin=" + origin +
                ", destination=" + destination +
                ", amount=" + amount +
                ", nonce=" + nonce +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, destination, nonce);
    }

    public static boolean verify(Transaction transaction) {
        String publicKey = transaction.getOrigin().getPubKey();
        return Cryptography.verify(publicKey, transaction.toString(), transaction.getSignature());
    }

}
