package com.dds.springitdlp.application.entities;

import com.dds.springitdlp.application.contracts.SmartContract;
import com.dds.springitdlp.cryptography.Cryptography;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Data
@NoArgsConstructor
public class Transaction implements Serializable {

    public static final double MINING_REWARD = 100.0;

    private String uuid;
    private Account origin;
    private Account destination;
    private double amount;
    private long timestamp;
    private String signature;
    private SmartContract smartContract;

    public Transaction(Account origin, Account destination, double amount) {
//        this.uuid = UUID.randomUUID().toString();
//        this.origin = origin;
//        this.destination = destination;
//        this.amount = amount;
//        this.timestamp = System.currentTimeMillis();
//        this.smartContract = null;
        this(origin, destination, amount, null);
    }

    public Transaction(Account origin, Account destination, double amount, SmartContract smartContract) {
        this.uuid = UUID.randomUUID().toString();
        this.origin = origin;
        this.destination = destination;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
        this.smartContract = smartContract;
    }

    /**
     * Special transaction for the reward miners get
     *
     * @param account - account that gets the reward
     * @return reward Transaction
     */
    public static Transaction REWARD_TRANSACTION(Account account) {
        return new Transaction(Account.SYSTEM_ACC(), account, Transaction.MINING_REWARD * 10);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "uuid='" + uuid + '\'' +
                ", origin=" + origin +
                ", destination=" + destination +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    public static boolean verify(Transaction transaction) {
        String publicKey = transaction.getOrigin().getPubKey();
        return Cryptography.verify(publicKey, transaction.toString(), transaction.getSignature());
    }

}
