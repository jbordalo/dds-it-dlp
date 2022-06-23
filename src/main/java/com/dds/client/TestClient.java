package com.dds.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class TestClient {
    public static void main(String[] args) throws UnrecoverableKeyException, CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, URISyntaxException, InterruptedException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Starting interactive Test Client (start with defaults - replica url localhost8080 and 12 accs?): Y/N?");
        Client client;
        if(in.readLine().equalsIgnoreCase("Y")){
            client = new Client();
        } else {
            System.out.println("Insert replica url:");
            String replicaURL = in.readLine();
            System.out.println("Insert number of accounts (max = 12):");
            int accs = Math.min(Client.MAX, Integer.parseInt(in.readLine()));
            client = new Client(accs, replicaURL);
        }
        System.out.println("Initializing blockchain... Acc0 will have transferable currency.");
        client.initBlockchain();
        HttpResponse<String> response;
        while(true) {
            System.out.print("> ");
            String[] command = in.readLine().split(" ");
            switch (command[0].toUpperCase()) {
                case "TRANSFER", "T":
                    response = client.sendTransaction(Integer.parseInt(command[1]), Integer.parseInt(command[2]), Double.parseDouble(command[3]));
                    System.out.println("TRANSACTION:" + response.statusCode());
                    break;
                case "MINE", "M":
                    response = client.requestMineAndProposeBlock(Integer.parseInt(command[1]));
                    System.out.println("MINING DONE: " + response.statusCode());
                    break;
                case "BALANCE", "B":
                    response = client.getBalance(Integer.parseInt(command[1]));
                    System.out.println("BALANCE OF ACC " + command[1] + " is " + response.body());
                    break;
                case "LEDGER", "L":
                    response = client.getLedger();
                    System.out.println("LEDGER:\n" +response.body());
                    break;
                case "GLOBAL", "G":
                    response = client.getGlobal();
                    System.out.println("THE SYSTEM HAS A GLOBAL VALUE OF "+ response.body());
                    break;
                case "TOTAL", "TV":
                    int accs[] = new int[command.length-1];
                    for(int i = 1; i< command.length; i++) {
                        accs[i-1] = Integer.parseInt(command[i]);
                    }
                    response = client.getTotal(accs);
                    System.out.println("COMBINED BALANCE: " + response.body());
                    break;
                case "EXTRACT", "E":
                    response = client.getExtract(Integer.parseInt(command[1]));
                    System.out.println("ACC " + command[1] + "'s EXTRACT:\n" + response.body());
                    break;
                case "EXIT", "X": return;
                default:
                    help();
                    break;
            }
        }
    }
    private static void help() {
        System.out.println("> HELP\nPossible commands:\nTRANSFER {src acc number} {dest acc number} {amount} (creates a transaction between the accounts)\nMINE {acc number} (account n requests for a block and mines it)\nBALANCE {acc number} (get the balance of account)\nLEDGER (gets the ledger)\nGLOBAL (returns the global balance of the blockchain) \nTOTAL {acc1} {acc2} ... {accn} (gets the aggregate balance of the list of accounts given)\nEXTRACT {acc number} (returns the account's extract)\nEXIT (Terminates execution)");
    }
}
