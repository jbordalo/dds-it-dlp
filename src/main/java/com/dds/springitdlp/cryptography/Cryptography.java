package com.dds.springitdlp.cryptography;

import com.dds.springitdlp.application.entities.Transaction;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Cryptography {
    public static boolean verify(String publicKey, String signedContent, String signatureString) {
        try {
            Signature signature = Signature.getInstance("SHA512withECDSA", "BC");
            byte[] encoded = Base64.decodeBase64(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey pkey = keyFactory.generatePublic(keySpec);
            signature.initVerify(pkey);
            signature.update(signedContent.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.decodeBase64(signatureString);
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | NoSuchProviderException |
                 InvalidKeySpecException e) {
            return false;
        }
    }
}
