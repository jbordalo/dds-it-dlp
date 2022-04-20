package com.dds.client;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Client {

    private static final String url = "https://localhost:8080";
    private static final HttpClient client = HttpClient.newBuilder().build();

    public static String getBalance(String accountId) throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = url + "/balance/" + accountId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String getLedger() throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = url + "/ledger" ;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String getGlobalLedgerValue() throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = url + "/globalLedgerValue";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String getTotalValue(String[] accounts) throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = url + "/totalValue";

        String accountsJSON = new ObjectMapper().writeValueAsString(accounts);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(accountsJSON))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static String getExtract(String accountId) throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = url + "/extract/" + accountId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public static void sendTransaction(Transaction transaction) throws URISyntaxException, IOException, InterruptedException {
        String reqUrl = url + "/sendTransaction";

        String transactionJSON = new ObjectMapper().writeValueAsString(transaction);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(reqUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(transactionJSON))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());
    }

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        sendTransaction(new Transaction(new Account("orig"), new Account("dest"), 10.0));

        System.out.println(getBalance("dest"));

        System.out.println(getLedger());

        System.out.println(getExtract("dest"));

        System.out.println(getGlobalLedgerValue());

        System.out.println(getTotalValue(new String[]{"orig", "dest"}));
    }
}