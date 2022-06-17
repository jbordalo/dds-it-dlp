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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class YCSBClient extends DB {

    Client client;
    @Override
    public void init() {
        try {
            client = new Client();
            client.initBlockchain();
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | CertificateException |
                 IOException | URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Status read(String table, String key, Set<String> fields, Map<String, ByteIterator> result) {
        return null;
    }

    @Override
    public Status scan(String table, String startkey, int recordcount, Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Status update(String table, String key, Map<String, ByteIterator> values) {
        return null;
    }

    @Override
    public Status insert(String table, String key, Map<String, ByteIterator> values) {
        return null;
    }

    @Override
    public Status delete(String table, String key) {
        throw new UnsupportedOperationException();
    }
}
