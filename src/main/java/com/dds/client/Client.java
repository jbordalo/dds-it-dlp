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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Client {
    private static final String URL = "https://localhost:8080";
    private static final String ALGORITHM = "SHA512withECDSA";
    private static PrivateKey key;
    private static PublicKey pubkey;

    private static final HttpClient client = HttpClient.newBuilder().build();


    public static byte[] sign(byte[] data) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, SignatureException, InvalidKeyException, IOException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        Signature signature = Signature.getInstance("SHA512withECDSA", "BC");

        signature.initSign(key, new SecureRandom());
        signature.update(data);

        return signature.sign();
    }

    public static String getBalance(String accountId) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException, UnrecoverableKeyException, CertificateException, KeyStoreException {
        String reqUrl = URL + "/balance?accountId=" + accountId;

        String signable = "GET " + reqUrl + " " + ALGORITHM;

        String signature = getSignature(signable);

        HttpRequest request = HttpRequest.newBuilder()
                .headers("signature", signature, "algorithm", ALGORITHM)
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String getLedger() throws URISyntaxException, IOException, InterruptedException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, KeyStoreException, NoSuchProviderException, InvalidKeyException {
        String reqUrl = URL + "/ledger" ;

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
                .headers("algorithm", "SHA512withECDSA")
                .uri(new URI(reqUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(accountsJSON))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }


    public static String getExtract(String accountId) throws URISyntaxException, IOException, InterruptedException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, KeyStoreException, NoSuchProviderException, InvalidKeyException {
        String reqUrl = URL + "/extract?accountId=" + accountId;

        String signable = "GET " + reqUrl + " " + ALGORITHM;

        String signature = getSignature(signable);

        HttpRequest request = HttpRequest.newBuilder()
                .headers("signature", signature, "algorithm", ALGORITHM)
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static void sendTransaction(Transaction transaction) throws URISyntaxException, IOException, InterruptedException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, KeyStoreException, NoSuchProviderException, InvalidKeyException {
        String reqUrl = URL + "/sendTransaction?accountId=" + transaction.getOrigin().getAccountId();

        String transactionJSON = new ObjectMapper().writeValueAsString(transaction);

        String signable = "POST " + reqUrl + " " + ALGORITHM;

        String signature = getSignature(signable);

        HttpRequest request = HttpRequest.newBuilder()
                .headers("signature", signature, "algorithm", ALGORITHM)
                .uri(new URI(reqUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(transactionJSON))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());
    }

    private static String getSignature(String signable) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, SignatureException, InvalidKeyException, IOException, KeyStoreException, CertificateException, UnrecoverableKeyException {
        return Base64.encodeBase64String(sign(signable.getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException, UnrecoverableKeyException, CertificateException, KeyStoreException {
        Security.addProvider(new BouncyCastleProvider());

        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        FileInputStream stream = new FileInputStream("config/keystores/keyStore");

        keyStore.load(stream, "ddsdds".toCharArray());

        stream.close();

        key = (PrivateKey) keyStore.getKey("dds", "ddsdds".toCharArray());
        pubkey = keyStore.getCertificate("dds").getPublicKey();

        String pubkey64 = Base64.encodeBase64URLSafeString(pubkey.getEncoded());

        sendTransaction(new Transaction(new Account(pubkey64), new Account("dest"), 10.0));

        System.out.println(getBalance(pubkey64));

        System.out.println(getLedger());

        System.out.println(getExtract(pubkey64));

        System.out.println(getGlobalLedgerValue());

        System.out.println(getTotalValue(new String[]{pubkey64, "dest"}));
    }
}