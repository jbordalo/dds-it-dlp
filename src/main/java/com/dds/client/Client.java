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
    private static final String ALGORITHM = "SHA512withECDSA";
    private static final HttpClient client = HttpClient.newBuilder().build();
    public static int MAX = 12;
    public static String URL = "https://localhost:8080";
    private static final String ENDORSER_URL = "https://localhost:8090";
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

    private static void initializeAccounts(KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        keys = new PrivateKey[MAX];
        accs = new Account[MAX];
        int count;
        try {
            FileInputStream fi = new FileInputStream("accData.txt");
            ObjectInputStream oi = new ObjectInputStream(fi);
            int saved = oi.readInt();
            for (count = 0; count < saved && count < MAX; count++) {
                accs[count] = (Account) oi.readObject();
                keys[count] = (PrivateKey) oi.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            count = 0;
        }

        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try {
            FileOutputStream f = new FileOutputStream("accData.txt");
            ObjectOutputStream o = new ObjectOutputStream(f);
            o.writeInt(MAX);
            for (int i = count; i < MAX; i++) {
                keys[i] = (PrivateKey) keyStore.getKey("dds" + i, "ddsdds".toCharArray());
                String pubKey64 = Base64.encodeBase64URLSafeString(keyStore.getCertificate("dds" + i).getPublicKey().getEncoded());
                String emailtime = i + "bacinta01@greatestemail.com" + System.currentTimeMillis() + new SecureRandom().nextInt();
                accs[i] = new Account(Base64.encodeBase64URLSafeString(hash.digest(emailtime.getBytes(StandardCharsets.UTF_8))) + pubKey64);
                o.writeObject(accs[i]);
                o.writeObject(keys[i]);
            }
            o.close();
            f.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void testSmartContracts() throws IOException, URISyntaxException, InterruptedException {
        // Create a non-malicious Smart Contract
        SmartContract contract = new BasicSmartContract();

        // Endorse it
        SmartContract smartContract = endorse(contract);

        // Try to register it
        RegisterResult result = registeredResponse(smartContract);
        System.out.println("Non-malicious SM " + result);
        assert(result.equals(RegisterResult.CONTRACT_REGISTERED));

        // Create a malicious Smart Contract
        contract = new WriterSmartContract();
        // Try to endorse it
        smartContract = endorse(contract);

        // Try to register it
        result = registeredResponse(smartContract);
        System.out.println("Malicious SMs\nWriter SM "+result);
        assert (result.equals(RegisterResult.CONTRACT_REJECTED));

        // Try to register unsigned contract
        SmartContract unsigned = new BasicSmartContract();
        result = registeredResponse(unsigned);
        System.out.println("Unsigned SM "+result);
        assert (result.equals(RegisterResult.CONTRACT_REJECTED));

        contract = new SocketSmartContract();
        endorse(contract);
        result = registeredResponse(contract);
        System.out.println("Socket Using SM "+result);
        assert (result.equals(RegisterResult.CONTRACT_REJECTED));

        contract = new MemoryHogSmartContract();
        endorse(contract);
        result = registeredResponse(contract);
        System.out.println("Memory Hog SM "+result);
        assert (result.equals(RegisterResult.CONTRACT_REJECTED));
    }

    private static SmartContract endorse(SmartContract smartContract) throws IOException, URISyntaxException, InterruptedException {
        HttpResponse<byte[]> response = processRequest(REQUEST.ENDORSE, smartContract, null);
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

    private static RegisterResult registeredResponse(SmartContract smartContract) throws IOException, URISyntaxException, InterruptedException {
        HttpResponse<String> response = processRequest(REQUEST.REGISTER_RESULT, smartContract, null);
        return new ObjectMapper().readValue(response.body(), RegisterResult.class);
    }

    private static HttpResponse processRequest(REQUEST REQUEST, Object obj, PrivateKey key) throws IOException, URISyntaxException, InterruptedException {
        String reqUrl = URL + REQUEST.getUrl();
        switch (REQUEST) {
            case GET_BLOCK, GET_TOTAL, PROPOSE_BLOCK -> {
                String json = new ObjectMapper().writeValueAsString(obj);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(reqUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                return client.send(request, HttpResponse.BodyHandlers.ofString());
            }
            case GET_GLOBAL, GET_LEDGER -> {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(reqUrl))
                        .GET()
                        .build();

                return client.send(request, HttpResponse.BodyHandlers.ofString());
            }
            case GET_BALANCE, GET_EXTRACT -> {
                reqUrl = reqUrl + obj; //obj is accId
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
                Transaction transaction = (Transaction) obj;
                reqUrl = reqUrl + transaction.getOrigin().getAccountId();

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
            case ENDORSE -> {
                HttpRequest request = buildSmartContractRequest(obj, reqUrl);
                return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            }
            case REGISTER_RESULT -> {
                HttpRequest request = buildSmartContractRequest(obj, reqUrl);
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            }
        }
        return null;
    }

    private static HttpRequest buildSmartContractRequest(Object obj, String reqUrl) throws URISyntaxException {
        byte[] bytes = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            bytes = bos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();
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

    public HttpResponse<String> requestMineAndProposeBlock(int acc) throws IOException, URISyntaxException, InterruptedException {
        HttpResponse<String> response = processRequest(REQUEST.GET_BLOCK, createBlockRequest(accs[acc], keys[acc]), null);
        if (response.statusCode() == 200) {
            Block b = new ObjectMapper().readValue(response.body(), Block.class);
            b = mineBlock(b);
            return processRequest(REQUEST.PROPOSE_BLOCK, b, null);
        }
        return response;
    }

    public HttpResponse<String> getBalance(int acc) throws IOException, URISyntaxException, InterruptedException {
        return processRequest(REQUEST.GET_BALANCE, accs[acc].getAccountId(), keys[acc]);
    }

    public HttpResponse<String> getLedger() throws IOException, URISyntaxException, InterruptedException {
        return processRequest(REQUEST.GET_LEDGER, null, null);
    }

    public HttpResponse<String> getGlobal() throws IOException, URISyntaxException, InterruptedException {
        return processRequest(REQUEST.GET_GLOBAL, null, null);
    }

    public HttpResponse<String> getTotal(int[] accnums) throws IOException, URISyntaxException, InterruptedException {
        String[] accounts = new String[accnums.length];
        for (int i = 0; i < accnums.length; i++) {
            accounts[i] = accs[accnums[i]].getAccountId();
        }
        return processRequest(REQUEST.GET_TOTAL, accounts, null);
    }

    public HttpResponse<String> getExtract(int acc) throws IOException, URISyntaxException, InterruptedException {
        return processRequest(REQUEST.GET_EXTRACT, accs[acc].getAccountId(), keys[acc]);
    }

    public void initBlockchain() throws IOException, URISyntaxException, InterruptedException {
        requestMineAndProposeBlock(0);
        for (int i = 1; i < MAX; i++) {
            processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[i], 12.50), keys[0]);
        }
        requestMineAndProposeBlock(0);
    }

    public HttpResponse<String> sendTransaction(int acc, int destAcc, double amount, boolean async) throws IOException, URISyntaxException, InterruptedException {
        if (async)
            return processRequest(REQUEST.SEND_ASYNC_TRANSACTION, new Transaction(accs[acc], accs[destAcc], amount), keys[acc]);
        return processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[acc], accs[destAcc], amount), keys[acc]);
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
        PROPOSE_BLOCK("/proposeBlock"),
        REGISTER_RESULT("/registerSmartContract"),
        ENDORSE("/endorse");

        private final String url;

        REQUEST(String s) {
            this.url = s;
        }

        String getUrl() {
            return url;
        }
    }


    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, KeyStoreException {
        Security.addProvider(new BouncyCastleProvider());

        KeyStore keyStore = Cryptography.initializeKeystore("config/keystores/keyStore", "ddsdds");

        initializeAccounts(keyStore);

        testSmartContracts();

        // Try to mine the first block (blockchain could be empty)
        System.out.println("Mining genesis");


        HttpResponse<String> response = processRequest(REQUEST.GET_BLOCK, createBlockRequest(accs[0], keys[0]), null);
        System.out.println(response.statusCode());
        if (response.statusCode() == 200) {
            Block b = new ObjectMapper().readValue(response.body(), Block.class);
            b = mineBlock(b);
            System.out.println(processRequest(REQUEST.PROPOSE_BLOCK, b, null).statusCode());
        }

        // Have the account that mined transfer money to more accounts
        // This creates money in some accounts for testing
        SmartContract smartContract = new BasicSmartContract();
        smartContract = endorse(smartContract);

        // We need to test if was accepted, depends on status code, check testContracts()
        RegisterResult res = registeredResponse(smartContract);

        String smartContractUuid;
        // We also check for REGISTER / REJECTED
        if (res == RegisterResult.CONTRACT_REGISTERED) {
            smartContractUuid = smartContract.getUuid();
            // This one should pass
            System.out.println(processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[1], 10.0, smartContractUuid), keys[0]).statusCode());
            // This one should fail
            System.out.println(processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[1], 8.5, smartContractUuid), keys[0]).statusCode());
        }

        // This one should fail
        System.out.println(processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[1], 8.5, "nada"), keys[0]).statusCode());
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
        if (response.statusCode() == 200) {
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
        System.out.println("Failed transaction below:");
        HttpResponse resp = processRequest(REQUEST.SEND_ASYNC_TRANSACTION, new Transaction(accs[0], accs[1], 5000.0), keys[0]);
        System.out.println(resp.statusCode());
        System.out.println(resp.body());

        System.out.println("Mining another block");
        response = processRequest(REQUEST.GET_BLOCK, createBlockRequest(accs[2], keys[2]), null);
        System.out.println(response.statusCode());
        if (response.statusCode() == 200) {
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
