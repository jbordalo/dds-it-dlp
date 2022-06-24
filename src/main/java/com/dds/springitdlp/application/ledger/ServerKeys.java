package com.dds.springitdlp.application.ledger;

import com.dds.springitdlp.cryptography.Cryptography;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Getter
@Component
public class ServerKeys {
    private final PrivateKey privateKey;
    private final Map<String, PublicKey> endorserKeys = new HashMap<>();

    public ServerKeys() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keyStore = Cryptography.initializeKeystore(System.getenv("SERVER_KEYSTORE"), System.getenv("SERVER_KEYSTORE_PW"));
        this.privateKey = (PrivateKey) keyStore.getKey(System.getenv("SERVER_KEYSTORE_ALIAS"), System.getenv("SERVER_KEYSTORE_PW").toCharArray());

        if (System.getenv("ENDORSERS_KEYSTORE") != null) {
            keyStore = Cryptography.initializeKeystore(System.getenv("ENDORSERS_KEYSTORE"), System.getenv("ENDORSERS_KEYSTORE_PW"));
            Enumeration<String> a = keyStore.aliases();
            if (a.hasMoreElements()) {
                String alias = a.nextElement();
                this.endorserKeys.put(alias, keyStore.getCertificate(alias).getPublicKey());
            }
        }
    }

    public PublicKey getEndorserKey(String endorserId) {
        return this.endorserKeys.get(System.getenv("ENDORSERS_KEYSTORE_ALIAS") + endorserId);
    }

}
