package com.dds.springitdlp.application.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.Base64;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
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

    private String signature;

    public static Transaction SYS_INIT(Account account) {
        return new Transaction(Account.SYSTEM_ACC(), account, Transaction.INITIAL_VALUE, 0, 0, null);
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
        String publicKey = Account.parse(transaction.getOrigin().getAccountId());
        try {
            Signature signature = Signature.getInstance("SHA512withECDSA", "BC");
            byte[] encoded = Base64.decodeBase64(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey pkey = keyFactory.generatePublic(keySpec);
            signature.initVerify(pkey);
            signature.update(transaction.toString().getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.decodeBase64(transaction.getSignature());
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | NoSuchProviderException |
                 InvalidKeySpecException e) {
            return false;
        }
    }

}
