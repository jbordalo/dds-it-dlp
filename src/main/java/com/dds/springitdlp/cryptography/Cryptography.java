package com.dds.springitdlp.cryptography;

import org.apache.commons.codec.binary.Base64;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
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
            e.printStackTrace();
            return false;
        }
    }

    public static boolean verify(PublicKey publicKey, String signedContent, String signatureString) {
        return Cryptography.verify(Base64.encodeBase64URLSafeString(publicKey.getEncoded()), signedContent, signatureString);
    }

    public static String sign(String data, PrivateKey key) {
        try {
            Signature signature = Signature.getInstance("SHA512withECDSA", "BC");
            signature.initSign(key, new SecureRandom());
            signature.update(data.getBytes(StandardCharsets.UTF_8));

            return Base64.encodeBase64URLSafeString(signature.sign());
        } catch (NoSuchAlgorithmException | NoSuchProviderException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String hash(String input) {
        MessageDigest hash;
        try {
            hash = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return Base64.encodeBase64URLSafeString(hash.digest(input.getBytes()));
    }

    public static KeyStore initializeKeystore(String path, String password) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        FileInputStream stream = new FileInputStream(path);

        keyStore.load(stream, password.toCharArray());

        stream.close();
        return keyStore;
    }
}
