package com.dds.client;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

public class Client {

    private static final int MAX = 4;
    private static final String URL = "https://localhost:8080";
    private static final String ALGORITHM = "SHA512withECDSA";
    private static final HttpClient client = HttpClient.newBuilder().build();
    private static PrivateKey[] keys;
    private static Account[] accs;

    public static byte[] sign(byte[] data, PrivateKey key) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, SignatureException, InvalidKeyException, IOException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        Signature signature = Signature.getInstance("SHA512withECDSA", "BC");

        signature.initSign(key, new SecureRandom());
        signature.update(data);

        return signature.sign();
    }

    public static String getBalance(String accountId, PrivateKey key) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException, UnrecoverableKeyException, CertificateException, KeyStoreException {
        String reqUrl = URL + "/balance?accountId=" + accountId;

        String signable = "GET " + reqUrl + " " + ALGORITHM;

        String signature = getSignature(signable, key);

        HttpRequest request = HttpRequest.newBuilder()
                .headers("signature", signature, "algorithm", ALGORITHM)
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String getLedger() throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = URL + "/ledger";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String getGlobalLedgerValue() throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = URL + "/globalLedgerValue";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String getTotalValue(String[] accounts) throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = URL + "/totalValue";

        String accountsJSON = new ObjectMapper().writeValueAsString(accounts);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(accountsJSON))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String getExtract(String accountId, PrivateKey key) throws URISyntaxException, IOException, InterruptedException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, KeyStoreException, NoSuchProviderException, InvalidKeyException {
        String reqUrl = URL + "/extract?accountId=" + accountId;

        String signable = "GET " + reqUrl + " " + ALGORITHM;

        String signature = getSignature(signable, key);

        HttpRequest request = HttpRequest.newBuilder()
                .headers("signature", signature, "algorithm", ALGORITHM)
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static void sendTransaction(Transaction transaction, PrivateKey key) throws URISyntaxException, IOException, InterruptedException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, KeyStoreException, NoSuchProviderException, InvalidKeyException {
        String reqUrl = URL + "/sendTransaction?accountId=" + transaction.getOrigin().getAccountId();

        String signable = transaction.toString();

        String signature = getSignature(signable, key);

        transaction.setSignature(signature);

        String transactionJSON = new ObjectMapper().writeValueAsString(transaction);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(transactionJSON))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());
    }

    private static String getSignature(String signable, PrivateKey key) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, SignatureException, InvalidKeyException, IOException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        return Base64.encodeBase64String(sign(signable.getBytes(StandardCharsets.UTF_8), key));
    }

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException, UnrecoverableKeyException, CertificateException, KeyStoreException {
        Security.addProvider(new BouncyCastleProvider());
        KeyStore keyStore = initializeKeystore();

        initializeAccounts(keyStore);

        for (int i = 0; i < MAX; i++) {
            int aux = (i + 1) % MAX;
            sendTransaction(new Transaction(accs[i], accs[aux], 10.0, new SecureRandom().nextInt(), System.currentTimeMillis(), null), keys[i]);
            System.out.println(getBalance(accs[i].getAccountId(), keys[i]));
        }

        System.out.println(getLedger());

        System.out.println(getExtract(accs[0].getAccountId(), keys[0]));

        System.out.println(getGlobalLedgerValue());

        System.out.println(getTotalValue(new String[]{accs[0].getAccountId(), accs[1].getAccountId()}));
    }

    private static void initializeAccounts(KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        keys = new PrivateKey[MAX];
        accs = new Account[MAX];

        MessageDigest hash = MessageDigest.getInstance("SHA-256");

        for (int i = 0; i < MAX; i++) {
            keys[i] = (PrivateKey) keyStore.getKey("dds" + i, "ddsdds".toCharArray());
            String pubKey64 = Base64.encodeBase64URLSafeString(keyStore.getCertificate("dds" + i).getPublicKey().getEncoded());
            String emailtime = i + "bacinta01@greatestemail.com" + System.currentTimeMillis() + new SecureRandom().nextInt();
            accs[i] = new Account(Base64.encodeBase64URLSafeString(hash.digest(emailtime.getBytes(StandardCharsets.UTF_8))) + pubKey64);
        }
    }

    private static KeyStore initializeKeystore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        FileInputStream stream = new FileInputStream("config/keystores/keyStore");

        keyStore.load(stream, "ddsdds".toCharArray());

        stream.close();
        return keyStore;
    }
}