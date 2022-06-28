package com.dds.client;

import site.ycsb.ByteIterator;
import site.ycsb.DB;
import site.ycsb.Status;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;

public class YCSBClient extends DB {

    Client client;
    boolean async = false;

    @Override
    public void init() {
        try {
            Properties properties = getProperties();
            async = Boolean.parseBoolean(properties.getProperty("toggleasync"));
            client = new Client(Integer.parseInt(properties.getProperty("maxaccounts", String.valueOf(Client.MAX))),
                    properties.getProperty("replicaurl", Client.URL), properties.getProperty("keystore"),
                    properties.getProperty("keystorepassword"), properties.getProperty("keystorealias"), properties.getProperty("savedaccdata"));
            client.initBlockchain();
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | CertificateException |
                 IOException | URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
        try {
            Integer k = Integer.valueOf(key);
            return mapStatus(client.getBalance(accRange(k)).statusCode());
        } catch (IOException | URISyntaxException | InterruptedException e) {
            return Status.ERROR;
        }
    }

    @Override
    public Status scan(String table, String startkey, int recordcount, Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Status update(String table, String key, Map<String, ByteIterator> values) {
        try {
            Integer k = Integer.valueOf(key);
            return mapStatus(client.requestMineAndProposeBlock(accRange(k)).statusCode());
        } catch (IOException | URISyntaxException | InterruptedException e) {
            return Status.ERROR;
        }
    }

    @Override
    public Status insert(String table, String key, Map<String, ByteIterator> values) {
        try {
            Integer k = Integer.valueOf(key);
            Integer dest = Integer.valueOf(values.get("destination").toString());
            return mapStatus(client.sendTransaction(accRange(k), accRange(dest), Double.parseDouble(values.get("value").toString()), async).statusCode());
        } catch (IOException | URISyntaxException | InterruptedException e) {
            return Status.ERROR;
        }
    }

    @Override
    public Status delete(String table, String key) {
        throw new UnsupportedOperationException();
    }

    private int accRange(int acc) {
        return Math.abs(acc % Client.MAX);
    }

    private Status mapStatus(int statusCode) {
        switch (statusCode) {
            case 200:
                return Status.OK;
            case 404:
                return Status.NOT_FOUND;
            case 403:
                return Status.FORBIDDEN;
            case 400:
                return Status.BAD_REQUEST;
            case 409:
                return Status.UNEXPECTED_STATE;
            default:
                return Status.ERROR;
        }
    }
}
