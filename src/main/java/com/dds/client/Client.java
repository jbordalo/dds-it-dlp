package com.dds.client;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class Client {
    private static final String URL = "https://localhost:8080";
    private static final String ALGORITHM = " SHA512withECDSA";
    private static final String PRIVATE_KEY = "MHQCAQEEIJwAjNAL4T9Vz7fvAB8UBBJNPNXY5MW1MbTSEHzHzWMKoAcGBSuBBAAKoUQDQgAEXR2ItGt8szt5EJ8BRJyf1+y2e6MQnodh3c6hv/6OF3Dh2zbkekR8BGN/hnpvMPlz7uwc/cf8c6rgNXzZE3LrxQ==";
    private static final HttpClient client = HttpClient.newBuilder().build();


    public static byte[] sign(byte[] data) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, SignatureException, InvalidKeyException {
        Signature signature = Signature.getInstance("SHA512withECDSA");
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");

        byte[] encoded = Base64.decodeBase64(PRIVATE_KEY);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        PrivateKey key = keyFactory.generatePrivate(keySpec);
        signature.initSign(key, new SecureRandom());
        signature.update(data);

        return signature.sign();
    }

    public static String getBalance(String accountId) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
        String reqUrl = URL + "/balance?accountId=" + accountId;

        String signable = "GET " + reqUrl + ALGORITHM;

        String signature = Base64.encodeBase64String(sign(signable.getBytes(StandardCharsets.UTF_8)));

        HttpRequest request = HttpRequest.newBuilder()
                .headers("signature", signature, "algorithm", ALGORITHM)
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String getLedger() throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = URL + "/ledger" ;

        String signature = null;

        HttpRequest request = HttpRequest.newBuilder()
                .headers("signature", signature, "algorithm", "SHA512withECDSA")
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String getGlobalLedgerValue() throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = URL + "/globalLedgerValue";

        HttpRequest request = HttpRequest.newBuilder()
                .headers("algorithm", "SHA512withECDSA")
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

    public static String getExtract(String accountId) throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = URL + "/extract?accountId=" + accountId;

        HttpRequest request = HttpRequest.newBuilder()
                .headers("algorithm", "SHA512withECDSA")
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static void sendTransaction(Transaction transaction) throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = URL + "/sendTransaction?accountId" + transaction.getOrigin().getAccountId();

        String transactionJSON = new ObjectMapper().writeValueAsString(transaction);

        HttpRequest request = HttpRequest.newBuilder()
                .headers("algorithm", "SHA512withECDSA")
                .uri(new URI(reqUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(transactionJSON))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());
    }

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
        Security.addProvider(new BouncyCastleProvider());
//        sendTransaction(new Transaction(new Account("orig"), new Account("dest"), 10.0));

        System.out.println(getBalance("dest"));

//        System.out.println(getLedger());
//
//        System.out.println(getExtract("dest"));
//
//        System.out.println(getGlobalLedgerValue());
//
//        System.out.println(getTotalValue(new String[]{"orig", "dest"}));
    }
}