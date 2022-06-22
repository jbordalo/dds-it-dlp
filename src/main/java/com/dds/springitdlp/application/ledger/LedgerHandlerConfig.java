package com.dds.springitdlp.application.ledger;

import com.dds.springitdlp.cryptography.Cryptography;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

@Getter
@Component
public class LedgerHandlerConfig {
    private final PrivateKey privateKey;

    public LedgerHandlerConfig() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keyStore = Cryptography.initializeKeystore(System.getenv("SERVER_KEYSTORE"), System.getenv("SERVER_KEYSTORE_PW"));
        this.privateKey = (PrivateKey) keyStore.getKey(System.getenv("SERVER_KEYSTORE_ALIAS"), System.getenv("SERVER_KEYSTORE_PW").toCharArray());
    }

}
