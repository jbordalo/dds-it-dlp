package com.dds.client;

import com.dds.springitdlp.application.contracts.*;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.RegisterResult;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.block.BlockRequest;
import com.dds.springitdlp.cryptography.Cryptography;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;

public class Client {

    private static final int MAX = 4;
    private static final String URL = "https://localhost:8080";
    private static final String ENDORSER_URL = "https://localhost:8090";
    private static final String ALGORITHM = "SHA512withECDSA";
    private static final HttpClient client = HttpClient.newBuilder().build();
    private static PrivateKey[] keys;
    private static Account[] accs;

    public static String getBalance(String accountId, PrivateKey key) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        String reqUrl = URL + "/balance?accountId=" + accountId;

        String signable = "GET " + reqUrl + " " + ALGORITHM;

        String signature = Cryptography.sign(signable, key);

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

    public static SmartContract endorse(SmartContract smartContract) throws IOException, URISyntaxException, InterruptedException {
        String reqUrl = ENDORSER_URL + "/endorse";

        byte[] bytes = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(smartContract);
            bytes = bos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            byte[] reply = response.body();

            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(reply);
                 ObjectInput objIn = new ObjectInputStream(byteIn)) {

                return (SmartContract) objIn.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static String getExtract(String accountId, PrivateKey key) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        String reqUrl = URL + "/extract?accountId=" + accountId;

        String signable = "GET " + reqUrl + " " + ALGORITHM;

        String signature = Cryptography.sign(signable, key);

        HttpRequest request = HttpRequest.newBuilder()
                .headers("signature", signature, "algorithm", ALGORITHM)
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static void sendTransaction(Transaction transaction, PrivateKey key) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        String reqUrl = URL + "/sendTransaction?accountId=" + transaction.getOrigin().getAccountId();

        String signable = transaction.toString();

        String signature = Cryptography.sign(signable, key);

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

    public static void sendAsyncTransaction(Transaction transaction, PrivateKey key) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        String reqUrl = URL + "/sendAsyncTransaction?accountId=" + transaction.getOrigin().getAccountId();

        String signable = transaction.toString();

        String signature = Cryptography.sign(signable, key);

        transaction.setSignature(signature);

        String transactionJSON = new ObjectMapper().writeValueAsString(transaction);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(transactionJSON))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());
        System.out.println(response.body());
    }

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException, UnrecoverableKeyException, CertificateException, KeyStoreException {
        Security.addProvider(new BouncyCastleProvider());

        KeyStore keyStore = Cryptography.initializeKeystore("config/keystores/keyStore", "ddsdds");

        initializeAccounts(keyStore);

        testSmartContracts();

        // Try to mine the first block (blockchain could be empty)
        System.out.println("Mining genesis");

        Block b = getBlock(accs[0], keys[0]);

        if (b == null) System.out.println("Couldn't get a block");

        proposeBlock(b);

        // Have the account that mined transfer money to more accounts
        // This creates money in some accounts for testing

        SmartContract smartContract = new BasicSmartContract();
        smartContract = endorse(smartContract);

        // We need to test if was accepted, depends on status code, check testContracts()
        RegisterResult res = register(smartContract);

        String smartContractUuid;
        // We also check for REGISTER / REJECTED
        if (res == RegisterResult.CONTRACT_REGISTERED) {
            smartContractUuid = smartContract.getUuid();
            // This one should pass
            sendTransaction(new Transaction(accs[0], accs[1], 10.0, smartContractUuid), keys[0]);
            // This one should fail
            sendTransaction(new Transaction(accs[0], accs[1], 8.5, smartContractUuid), keys[0]);
        }

        // This one should fail
        sendTransaction(new Transaction(accs[0], accs[1], 8.5, "nada"), keys[0]);

        System.out.println("Doing a communist");
        for (int i = 1; i < MAX; i++) {
            // Doing 4 * 12.5 instead of 50 so we can add extra transactions and are able to mine a block
            sendTransaction(new Transaction(accs[0], accs[i], 12.5), keys[0]);
            sendTransaction(new Transaction(accs[0], accs[i], 12.5), keys[0]);
            sendTransaction(new Transaction(accs[0], accs[i], 12.5), keys[0]);
            sendTransaction(new Transaction(accs[0], accs[i], 12.5), keys[0]);
        }

        System.out.println("Mining new block");

        b = getBlock(accs[0], keys[0]);

        if (b == null) System.out.println("Couldn't get a block");

        proposeBlock(b);

        System.out.println(getExtract(accs[0].getAccountId(), keys[0]));

        System.out.println("One for all");
        for (int i = 0; i < MAX; i++) {
            int aux = (i + 1) % MAX;
            sendTransaction(new Transaction(accs[i], accs[aux], 10.0), keys[i]);
            sendTransaction(new Transaction(accs[i], accs[aux], 10.0), keys[i]);
            sendTransaction(new Transaction(accs[i], accs[aux], 10.0), keys[i]);
            Transaction transaction = new Transaction(accs[i], accs[aux], 10.0);
            sendTransaction(transaction, keys[i]);
            System.out.println("Failed transaction below:");
            sendTransaction(transaction, keys[i]);
        }

        System.out.println(getExtract(accs[0].getAccountId(), keys[0]));

        System.out.println(getGlobalLedgerValue());

        System.out.println(getTotalValue(new String[]{accs[0].getAccountId(), accs[1].getAccountId()}));


        System.out.println("One for all async");
        for (int i = 1; i < MAX; i++) {
            sendAsyncTransaction(new Transaction(accs[0], accs[i], 5.0), keys[0]);
        }
        System.out.println("Failed transaction below:");
        sendAsyncTransaction(new Transaction(accs[0], accs[1], 5000.0), keys[0]);

        System.out.println("Mining another block");
        b = getBlock(accs[2], keys[2]);

        if (b == null) System.out.println("Couldn't get a block");

        proposeBlock(b);

        System.out.println(getLedger());

        System.out.println("Final balances");
        for (int i = 0; i < MAX; i++) {
            System.out.println(getBalance(accs[i].getAccountId(), keys[i]));
        }
    }

    private static void testSmartContracts() throws IOException, URISyntaxException, InterruptedException {
        // Create a non-malicious Smart Contract
        SmartContract contract = new BasicSmartContract();

        // Endorse it
        SmartContract smartContract = endorse(contract);

        // Try to register it
        System.out.println(register(smartContract));

        // Create a malicious Smart Contract
        contract = new WriterSmartContract();
        // Try to endorse it
        smartContract = endorse(contract);

        // Try to register it
        System.out.println(register(smartContract));

        // Try to register unsigned contract
        SmartContract unsigned = new BasicSmartContract();
        System.out.println(register(unsigned));

        contract = new SocketSmartContract();
        endorse(contract);

        contract = new MemoryHogSmartContract();
        endorse(contract);
    }

    private static RegisterResult register(SmartContract smartContract) throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = URL + "/registerSmartContract";

        byte[] bytes = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(smartContract);
            bytes = bos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return new ObjectMapper().readValue(response.body(), RegisterResult.class);
    }

    private static void proposeBlock(Block block) throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = URL + "/proposeBlock";

        String blockJSON = new ObjectMapper().writeValueAsString(block);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(blockJSON))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());
    }

    private static Block getBlock(Account account, PrivateKey key) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException, InvalidKeyException {
        String reqUrl = URL + "/block";

        // Make a block request
        BlockRequest blockRequest = new BlockRequest(System.currentTimeMillis(), new SecureRandom().nextInt(), account, "");

        String signable = blockRequest.toString();

        String signature = Cryptography.sign(signable, key);

        blockRequest.setSignature(signature);

        String blockRequestJSON = new ObjectMapper().writeValueAsString(blockRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(blockRequestJSON))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());

        if (response.statusCode() != 200) return null;

        Block block = new ObjectMapper().readValue(response.body(), Block.class);

        return mineBlock(block);
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
}
