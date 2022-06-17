package com.dds.client;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.block.BlockRequest;
import com.dds.springitdlp.cryptography.Cryptography;
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
import java.security.cert.CertificateException;
public class Client {
    private static final String ALGORITHM = "SHA512withECDSA";
    private static final HttpClient client = HttpClient.newBuilder().build();
    private static int MAX = 4;
    private static String URL = "https://localhost:8080";
    private static PrivateKey[] keys;
    private static Account[] accs;

    public Client() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        Security.addProvider(new BouncyCastleProvider());
        initializeAccounts(Cryptography.initializeKeystore("config/keystores/keyStore", "ddsdds"));
    }

    public Client(int maxAccs, String replicaURL) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        MAX = maxAccs;
        URL = replicaURL;

        Security.addProvider(new BouncyCastleProvider());
        initializeAccounts(Cryptography.initializeKeystore("config/keystores/keyStore", "ddsdds"));
    }

    private enum REQUEST {
        GET_BALANCE("/balance?accountId="),
        GET_LEDGER("/ledger"),
        GET_GLOBAL("/globalLedgerValue"),
        GET_TOTAL("/totalValue"),
        GET_EXTRACT("/extract?accountId="),
        SEND_TRANSACTION("/sendTransaction?accountId="),
        SEND_ASYNC_TRANSACTION("/sendAsyncTransaction?accountId="),
        GET_BLOCK("/block"),
        PROPOSE_BLOCK("/proposeBlock");

        private String url;

        REQUEST(String s) {
            this.url = s;
        }

        String getUrl(){
            return url;
        }
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

    private static HttpResponse<String> processRequest(REQUEST REQUEST, Object obj, PrivateKey key) throws IOException, URISyntaxException, InterruptedException {
        switch (REQUEST) {
            case GET_BLOCK, GET_TOTAL, PROPOSE_BLOCK -> {
                String reqUrl = URL + REQUEST.getUrl();
                String json = new ObjectMapper().writeValueAsString(obj);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(reqUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                return client.send(request, HttpResponse.BodyHandlers.ofString());
            }
            case GET_GLOBAL, GET_LEDGER -> {
                String reqUrl = URL + REQUEST.getUrl();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(reqUrl))
                        .GET()
                        .build();

                return client.send(request, HttpResponse.BodyHandlers.ofString());
            }
            case GET_BALANCE, GET_EXTRACT -> {
                String accountId = (String) obj;
                String reqUrl = URL + REQUEST.getUrl() + accountId;
                String signable = "GET " + reqUrl + " " + ALGORITHM;
                String signature = Cryptography.sign(signable, key);

                HttpRequest request = HttpRequest.newBuilder()
                        .headers("signature", signature, "algorithm", ALGORITHM)
                        .uri(new URI(reqUrl))
                        .GET()
                        .build();
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            }
            case SEND_TRANSACTION, SEND_ASYNC_TRANSACTION -> {
                Transaction transaction = (Transaction)obj;
                String reqUrl = URL + REQUEST.getUrl() + transaction.getOrigin().getAccountId();

                String signature = Cryptography.sign(transaction.toString(), key);
                transaction.setSignature(signature);

                String json = new ObjectMapper().writeValueAsString(transaction);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(reqUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                return client.send(request, HttpResponse.BodyHandlers.ofString());
            }
        }
        return null;
    }

    private static BlockRequest createBlockRequest(Account account, PrivateKey key) {
        BlockRequest blockRequest = new BlockRequest(System.currentTimeMillis(), new SecureRandom().nextInt(), account, "");
        String signable = blockRequest.toString();
        String signature = Cryptography.sign(signable, key);
        blockRequest.setSignature(signature);
        return blockRequest;
    }

    private static Block mineBlock(Block block) {
        int i = 0;
        while (true) {
            block.getHeader().setNonce(i);
            if (Block.checkBlock(block)) break;
            i++;
        }
        return block;
    }


    public void initBlockchain() throws IOException, URISyntaxException, InterruptedException {
        requestMineAndProposeBlock(accs[0], keys[0]);
        HttpResponse<String> response;
        for (int i = 1; i < MAX; i++) {
            for(int j = 0; j < MAX; j++) {
                processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[i], Transaction.MINING_REWARD*10/((MAX+1)*2)), keys[0]);
            }
        }
        requestMineAndProposeBlock(accs[0], keys[0]);
    }

    private void requestMineAndProposeBlock(Account acc, PrivateKey key) throws IOException, URISyntaxException, InterruptedException {
        HttpResponse<String> response = processRequest(REQUEST.GET_BLOCK, createBlockRequest(acc, key), null);
        if (response.statusCode() == 200){
            Block b = new ObjectMapper().readValue(response.body(), Block.class);
            b = mineBlock(b);
            processRequest(REQUEST.PROPOSE_BLOCK, b, null);
        }
    }

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, KeyStoreException {
        Security.addProvider(new BouncyCastleProvider());

        KeyStore keyStore = Cryptography.initializeKeystore("config/keystores/keyStore", "ddsdds");

        initializeAccounts(keyStore);

        // Try to mine the first block (blockchain could be empty)
        System.out.println("Mining genesis");


        HttpResponse<String> response = processRequest(REQUEST.GET_BLOCK, createBlockRequest(accs[0], keys[0]), null);
        System.out.println(response.statusCode());
        if (response.statusCode() == 200){
            Block b = new ObjectMapper().readValue(response.body(), Block.class);
            b = mineBlock(b);
            System.out.println(processRequest(REQUEST.PROPOSE_BLOCK, b, null).statusCode());
        }

        // Have the account that mined transfer money to more accounts
        // This creates money in some accounts for testing
        System.out.println("Doing a communist");
        for (int i = 1; i < MAX; i++) {
            // Doing 4 * 12.5 instead of 50 so we can add extra transactions and are able to mine a block
            System.out.println(processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[i], 12.5), keys[0]).statusCode());
            System.out.println(processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[i], 12.5), keys[0]).statusCode());
            System.out.println(processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[i], 12.5), keys[0]).statusCode());
            System.out.println(processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[i], 12.5), keys[0]).statusCode());
        }

        System.out.println("Mining new block");

        response = processRequest(REQUEST.GET_BLOCK, createBlockRequest(accs[0], keys[0]), null);
        System.out.println(response.statusCode());
        if (response.statusCode() == 200){
            Block b = new ObjectMapper().readValue(response.body(), Block.class);
            b = mineBlock(b);
            System.out.println(processRequest(REQUEST.PROPOSE_BLOCK, b, null).statusCode());
        }


        System.out.println(processRequest(REQUEST.GET_EXTRACT, accs[0].getAccountId(), keys[0]).body());

        System.out.println("One for all");
        for (int i = 0; i < MAX; i++) {
            int aux = (i + 1) % MAX;
            System.out.println(processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[i], accs[aux], 10.0), keys[i]).statusCode());
            System.out.println(processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[i], accs[aux], 10.0), keys[i]).statusCode());
            System.out.println(processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[i], accs[aux], 10.0), keys[i]).statusCode());
            Transaction transaction = new Transaction(accs[i], accs[aux], 10.0);
            System.out.println(processRequest(REQUEST.SEND_TRANSACTION, transaction, keys[i]).statusCode());
            System.out.println("Failed transaction below:");
            System.out.println(processRequest(REQUEST.SEND_TRANSACTION, transaction, keys[i]).statusCode());
        }

        System.out.println(processRequest(REQUEST.GET_EXTRACT, accs[0].getAccountId(), keys[0]).body());

        System.out.println(processRequest(REQUEST.GET_GLOBAL, null, null).body());

        System.out.println(processRequest(REQUEST.GET_TOTAL, new String[]{accs[0].getAccountId(), accs[1].getAccountId()}, null).body());


        System.out.println("One for all async");
        for (int i = 1; i < MAX; i++) {
            HttpResponse resp = processRequest(REQUEST.SEND_ASYNC_TRANSACTION, new Transaction(accs[0], accs[i], 5.0), keys[0]);
            System.out.println(resp.statusCode());
            System.out.println(resp.body());
        }

        System.out.println("Mining another block");
        response = processRequest(REQUEST.GET_BLOCK, createBlockRequest(accs[2], keys[2]), null);
        System.out.println(response.statusCode());
        if (response.statusCode() == 200){
            Block b = new ObjectMapper().readValue(response.body(), Block.class);
            b = mineBlock(b);
            processRequest(REQUEST.PROPOSE_BLOCK, b, null);
        }

        System.out.println(processRequest(REQUEST.GET_LEDGER, null, null).body());

        System.out.println("Final balances");
        for (int i = 0; i < MAX; i++) {
            System.out.println(processRequest(REQUEST.GET_BALANCE, accs[i].getAccountId(), keys[i]).body());
        }
    }


}
