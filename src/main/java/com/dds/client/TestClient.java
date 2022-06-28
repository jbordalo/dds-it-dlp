package com.dds.client;

import lombok.NonNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class TestClient {

    public static void main(String[] args) throws UnrecoverableKeyException, CertificateException, URISyntaxException, IOException, NoSuchAlgorithmException, KeyStoreException, InterruptedException {

        int max = Integer.parseInt(defaultWhenNull(System.getenv("MAX_ACCS"), String.valueOf(Client.MAX)));
        String endorser_url = defaultWhenNull(System.getenv("ENDORSER_URL"), Client.ENDORSER_URL);
        String url = defaultWhenNull(System.getenv("REPLICA_URL"), Client.URL);

        Client client = new Client(max, url, endorser_url, System.getenv("KEYSTORE"), System.getenv("KEYSTORE_PW"), System.getenv("KEYSTORE_ALIAS"), System.getenv("ACC_SAVE_FILE"));

        client.fullTest();
    }

    private static <T> T defaultWhenNull(@Nullable T object, @NonNull T def) {
        return (object == null) ? def : object;
    }

}
