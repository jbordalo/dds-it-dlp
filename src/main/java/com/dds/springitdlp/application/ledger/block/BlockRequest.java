package com.dds.springitdlp.application.ledger.block;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import lombok.*;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

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
        try {
            Signature signature = Signature.getInstance("SHA512withECDSA", "BC");
            byte[] encoded = Base64.decodeBase64(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey pkey = keyFactory.generatePublic(keySpec);
            signature.initVerify(pkey);
            signature.update(blockRequest.toString().getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.decodeBase64(blockRequest.getSignature());
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | NoSuchProviderException |
                 InvalidKeySpecException e) {
            return false;
        }
    }
}
