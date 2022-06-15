package com.dds.springitdlp.application.ledger.block;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.cryptography.Cryptography;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BlockRequest {
    private long timestamp;
    private long nonce;
    private Account account;
    @Setter
    private String signature;

    @Override
    public String toString() {
        return "BlockRequest{" +
                "timestamp=" + timestamp +
                ", nonce=" + nonce +
                ", account='" + account + '\'' +
                '}';
    }

    public static boolean verify(BlockRequest blockRequest) {
        String publicKey = blockRequest.getAccount().getPubKey();
        return Cryptography.verify(publicKey, blockRequest.toString(), blockRequest.getSignature());
    }
}
