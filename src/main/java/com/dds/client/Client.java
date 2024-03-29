package com.dds.client;

import com.dds.springitdlp.application.contracts.*;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.block.BlockRequest;
import com.dds.springitdlp.application.results.AsyncTransactionResult;
import com.dds.springitdlp.application.results.RegisterResult;
import com.dds.springitdlp.application.results.TransactionResultStatus;
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
    public static String ENDORSER_URL = "https://localhost:8090";
    private static PrivateKey[] keys;
    private static Account[] accs;

    public Client(String keystore, String password, String keystoreAlias, String accData) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        Security.addProvider(new BouncyCastleProvider());
        initializeAccounts(Cryptography.initializeKeystore(keystore, password), accData, keystoreAlias, password);
    }

    public Client(int maxAccs, String replicaURL, String keystore, String password, String keystoreAlias, String accData) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        MAX = maxAccs;
        URL = replicaURL;

        Security.addProvider(new BouncyCastleProvider());
        initializeAccounts(Cryptography.initializeKeystore(keystore, password), accData, keystoreAlias, password);
    }

    public Client(int maxAccs, String replicaURL, String endorserUrl, String keystore, String password, String keystoreAlias, String accData) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        this(maxAccs, replicaURL, keystore, password, keystoreAlias, accData);
        ENDORSER_URL = endorserUrl;
    }

    private void initializeAccounts(KeyStore keyStore, String accData, String keystoreAlias, String keystoreAliasPassword) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        keys = new PrivateKey[MAX];
        accs = new Account[MAX];

        try {
            FileInputStream fi = new FileInputStream(accData);
            ObjectInputStream oi = new ObjectInputStream(fi);
            int saved = oi.readInt();
            for (int count = 0; count < saved && count < MAX; count++) {
                accs[count] = (Account) oi.readObject();
                keys[count] = (PrivateKey) oi.readObject();
            }
            oi.close();
            fi.close();
            return;
        } catch (IOException | ClassNotFoundException ignored) {
        }

        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        try {
            FileOutputStream f = new FileOutputStream(accData, true);
            ObjectOutputStream o = new ObjectOutputStream(f);
            for (int i = 0; i < MAX; i++) {
                o.writeInt(MAX);
                keys[i] = (PrivateKey) keyStore.getKey(keystoreAlias + i, keystoreAliasPassword.toCharArray());
                String pubKey64 = Base64.encodeBase64URLSafeString(keyStore.getCertificate(keystoreAlias + i).getPublicKey().getEncoded());
                String emailTimeRnd = i + "dds-it-dlp@csd.com" + System.currentTimeMillis() + new SecureRandom().nextInt();
                accs[i] = new Account(Base64.encodeBase64URLSafeString(hash.digest(emailTimeRnd.getBytes(StandardCharsets.UTF_8))) + pubKey64);
                o.writeObject(accs[i]);
                o.writeObject(keys[i]);
            }
            o.close();
            f.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void testSmartContracts() throws IOException, URISyntaxException, InterruptedException {
        // Create a non-malicious Smart Contract
        SmartContract contract = new BasicSmartContract();

        // Endorse it
        SmartContract smartContract = endorse(contract);

        // Try to register it
        RegisterResult result = registeredResponse(smartContract);
        System.out.println("Non-malicious SM " + result);
        assert (result.equals(RegisterResult.CONTRACT_REGISTERED));

        // Create a malicious Smart Contract
        contract = new WriterSmartContract();
        // Try to endorse it
        smartContract = endorse(contract);

        // Try to register it
        result = registeredResponse(smartContract);
        System.out.println("Malicious SMs\nWriter SM " + result);
        assert (result.equals(RegisterResult.CONTRACT_REJECTED));

        // Try to register unsigned contract
        SmartContract unsigned = new BasicSmartContract();
        result = registeredResponse(unsigned);
        System.out.println("Unsigned SM " + result);
        assert (result.equals(RegisterResult.CONTRACT_REJECTED));

        contract = new SocketSmartContract();
        endorse(contract);
        result = registeredResponse(contract);
        System.out.println("Socket Using SM " + result);
        assert (result.equals(RegisterResult.CONTRACT_REJECTED));

        contract = new MemoryHogSmartContract();
        endorse(contract);
        result = registeredResponse(contract);
        System.out.println("Memory Hog SM " + result);
        assert (result.equals(RegisterResult.CONTRACT_REJECTED));
    }

    private SmartContract endorse(SmartContract smartContract) throws IOException, URISyntaxException, InterruptedException {
        HttpResponse<byte[]> response = processRequest(REQUEST.ENDORSE, smartContract, null);
        assert response != null;
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

    private RegisterResult registeredResponse(SmartContract smartContract) throws IOException, URISyntaxException, InterruptedException {
        HttpResponse<String> response = processRequest(REQUEST.REGISTER_RESULT, smartContract, null);
        assert response != null;
        return new ObjectMapper().readValue(response.body(), RegisterResult.class);
    }

    private HttpResponse processRequest(REQUEST REQUEST, Object obj, PrivateKey key) throws IOException, URISyntaxException, InterruptedException {
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
                reqUrl = reqUrl + obj; // obj is accId
                String toSign = "GET " + reqUrl + " " + ALGORITHM;
                String signature = Cryptography.sign(toSign, key);

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
                reqUrl = ENDORSER_URL + REQUEST.getUrl();
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

    private HttpRequest buildSmartContractRequest(Object obj, String reqUrl) throws URISyntaxException {
        byte[] bytes = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            bytes = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert bytes != null;

        return HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();
    }

    private BlockRequest createBlockRequest(Account account, PrivateKey key) {
        BlockRequest blockRequest = new BlockRequest(System.currentTimeMillis(), new SecureRandom().nextInt(), account, "");
        String toSign = blockRequest.toString();
        String signature = Cryptography.sign(toSign, key);
        blockRequest.setSignature(signature);
        return blockRequest;
    }

    private void mineBlock(Block block) {
        int i = 0;
        while (true) {
            block.getHeader().setNonce(i);
            if (Block.checkBlock(block)) break;
            i++;
        }
    }

    /**
     * IN THE CONTEXT OF THE TESTER, use when we know the block exists
     *
     * @param acc - Miner account
     * @return http response
     */
    public HttpResponse<String> requestMineAndProposeBlock(int acc) throws IOException, URISyntaxException, InterruptedException {
        HttpResponse<String> response = processRequest(REQUEST.GET_BLOCK, createBlockRequest(accs[acc], keys[acc]), null);

        assert response != null && response.statusCode() == 200;

        if (response.statusCode() == 200) {
            Block b = new ObjectMapper().readValue(response.body(), Block.class);
            mineBlock(b);
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

    public HttpResponse<String> getTotal(int[] accountIndices) throws IOException, URISyntaxException, InterruptedException {
        String[] accounts = new String[accountIndices.length];

        for (int i = 0; i < accountIndices.length; i++) {
            accounts[i] = accs[accountIndices[i]].getAccountId();
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
            processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[i], 6.50), keys[0]);
        }
        for (int i = 0; i < 2 * MAX / Block.MIN_TRANSACTIONS_BLOCK; i++) {
            requestMineAndProposeBlock(0);
        }
        for (int i = 1; i < MAX; i++) {
            processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[i], 6.50), keys[0]);
            processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[i], 4.50), keys[0]);
            processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[i], 2.50), keys[0]);
            processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[i], 2.50), keys[0]);
        }
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


    public void fullTest() throws URISyntaxException, IOException, InterruptedException {
        Security.addProvider(new BouncyCastleProvider());

        double[] balances = new double[MAX];
        int countT = 0;

        testSmartContracts();

        // Try to mine the first block (blockchain could be empty)
        System.out.println("Mining first block (genesis - if blockchain is empty)");

        HttpResponse<String> response = processRequest(REQUEST.GET_BLOCK, createBlockRequest(accs[0], keys[0]), null);

        assert (response != null);

        System.out.println("REQUEST FOR BLOCK: " + response.statusCode());
        if (response.statusCode() == 200) {
            Block b = new ObjectMapper().readValue(response.body(), Block.class);
            mineBlock(b);
            HttpResponse<String> res = processRequest(REQUEST.PROPOSE_BLOCK, b, null);
            assert res != null;
            System.out.println("PROPOSED BLOCK: " + res.statusCode());
        }

        System.out.println("Check balances: ");
        for (int i = 0; i < MAX; i++) {
            response = getBalance(i);
            assert response != null;
            System.out.println(response.body());
            balances[i] = Double.parseDouble(response.body());
            System.out.println("acc " + i + " - " + accs[i].getAccountId());
            assert balances[i] >= 0.0;
        }

        SmartContract smartContract = new BasicSmartContract();

        smartContract = endorse(smartContract);

        assert (smartContract != null);

        RegisterResult res = registeredResponse(smartContract);
        System.out.println("Non-malicious SM " + res);

        assert (res.equals(RegisterResult.CONTRACT_REGISTERED));

        String smartContractUuid = smartContract.getUuid();

        // This one should pass
        HttpResponse<String> smartContractResponse = processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[1], 10.0, smartContractUuid), keys[0]);

        assert smartContractResponse != null;

        countT++;
        balances[0] -= 10.0;
        balances[1] += 10.0;
        System.out.println("Sent transaction with the non-malicious SM - should pass: " + smartContractResponse.statusCode());
        assert (smartContractResponse.statusCode() == 200);

        // This one should fail
        smartContractResponse = processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[1], 8.5, smartContractUuid), keys[0]);
        assert smartContractResponse != null;
        System.out.println("Sent transaction with the non-malicious SM - should fail: " + smartContractResponse.statusCode());
        assert (smartContractResponse.statusCode() == 400);


        // This one should fail
        smartContractResponse = processRequest(REQUEST.SEND_TRANSACTION, new Transaction(accs[0], accs[1], 8.5, "non-existent"), keys[0]);
        assert smartContractResponse != null && (smartContractResponse.statusCode() == 400);
        System.out.println("Sent transaction with a random string as a SM Uuid - should fail: " + smartContractResponse.statusCode());

        for (int i = 1; i < MAX; i++) {
            for (int j = 0; j < Block.MIN_TRANSACTIONS_BLOCK / MAX + 1; j++) {
                response = sendTransaction(0, i, 12.5, false);
                assert response != null && response.statusCode() == 200;
                countT++;
                System.out.println("transaction status: " + response.statusCode());
                balances[0] -= 12.5;
                balances[i] += 12.5;
            }
        }

        System.out.println("Mining new block");
        response = requestMineAndProposeBlock(0);

        assert response != null;
        System.out.println("BLOCK PROPOSAL " + response.statusCode());
        assert response.statusCode() == 200;
        balances[0] += Transaction.MINING_REWARD;
        countT -= Block.MIN_TRANSACTIONS_BLOCK;


        for (int i = 1; i < Block.MIN_TRANSACTIONS_BLOCK / (Block.MIN_TRANSACTIONS_BLOCK / MAX + 1) + 1 && i < MAX; i++) {
            response = getBalance(i);
            assert response != null && response.statusCode() == 200 && balances[i] == Double.parseDouble(response.body());
            System.out.println("status " + response.statusCode() + " balance " + balances[i] + " " + response.body());
        }

        response = getTotal(new int[]{1, 2});

        assert response != null && balances[1] + balances[2] == Double.parseDouble(response.body());

        for (int i = 0; i < MAX; i++) {
            int aux = (i + 1) % MAX;
            System.out.println("4 x transaction from " + i + " to " + aux);
            for (int j = 0; j < 3; j++) {
                response = sendTransaction(i, aux, 10.0, false);

                assert response != null;
                System.out.println(response.statusCode());
                if (response.statusCode() == 200) countT++;
            }

            Transaction transaction = new Transaction(accs[i], accs[aux], 10.0);

            response = processRequest(REQUEST.SEND_TRANSACTION, transaction, keys[i]);

            assert response != null;

            if (response.statusCode() == 200) countT++;

            System.out.println(response.statusCode());

            response = processRequest(REQUEST.SEND_TRANSACTION, transaction, keys[i]);

            assert response != null;

            System.out.println("repeated transaction: " + response.statusCode());

            assert response.statusCode() >= 400;
        }

        for (int i = 1; i < MAX; i++) {
            response = sendTransaction(0, i, 5.0, true);

            assert response != null;

            System.out.println(response.statusCode());
            if (response.statusCode() != 408) {
                AsyncTransactionResult asyncRes = new ObjectMapper().readValue(response.body(), AsyncTransactionResult.class);
                TransactionResultStatus stats = asyncRes.getResults().get(0).getResult();
                System.out.println(stats);
                assert i >= Block.MIN_TRANSACTIONS_BLOCK / (Block.MIN_TRANSACTIONS_BLOCK / MAX + 1) || asyncRes.getResults().get(0).getResult().equals(TransactionResultStatus.OK_TRANSACTION);
                if (stats.equals(TransactionResultStatus.OK_TRANSACTION)) countT++;
            } else {
                countT++;
            }
            System.out.println(response.body());
        }
        System.out.println("Failed transaction below:");
        response = sendTransaction(0, 1, 5000.0, true);

        assert response != null;
        if (response.statusCode() == 200) {
            AsyncTransactionResult asyncRes = new ObjectMapper().readValue(response.body(), AsyncTransactionResult.class);
            System.out.println("Failed transaction " + asyncRes.getResults().get(0).getResult());
            assert asyncRes.getResults().get(0).getResult().equals(TransactionResultStatus.FAILED_TRANSACTION);
        }
        System.out.println(response.body());

        System.out.println("Mining another block");

        response = requestMineAndProposeBlock(2);

        assert response != null && response.statusCode() == 200;
        balances[2] += Transaction.MINING_REWARD;
        countT -= Block.MIN_TRANSACTIONS_BLOCK;

        response = getLedger();

        assert (response != null);
        Ledger l = new ObjectMapper().readValue(response.body(), Ledger.class);
        assert l.getBlockchain().size() == 3;
        System.out.println(response.body());
        System.out.println("transaction count/ :" + countT);
        for (int i = 0; i < countT / Block.MIN_TRANSACTIONS_BLOCK; i++) {
            response = requestMineAndProposeBlock(0);
            assert response != null && response.statusCode() == 200;
            System.out.println("Propose block " + response.statusCode());
            balances[0] += Transaction.MINING_REWARD;
            countT -= Block.MIN_TRANSACTIONS_BLOCK;
        }

        response = processRequest(REQUEST.GET_BLOCK, createBlockRequest(accs[0], keys[0]), null);

        assert response != null && response.statusCode() != 200;
        System.out.println("STATUS: " + response.statusCode());
        System.out.println(response.statusCode());
    }
}
