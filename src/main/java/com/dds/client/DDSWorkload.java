package com.dds.client;

import site.ycsb.ByteIterator;
import site.ycsb.DB;
import site.ycsb.Status;
import site.ycsb.StringByteIterator;
import site.ycsb.generator.ExponentialGenerator;
import site.ycsb.workloads.CoreWorkload;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DDSWorkload extends CoreWorkload {

    int nextKeynum() {
        int keynum;
        if (keychooser instanceof ExponentialGenerator) {
            do {
                keynum = (int) (transactioninsertkeysequence.lastValue() - keychooser.nextValue().intValue());
            } while (keynum < 0);
        } else {
            do {
                keynum = keychooser.nextValue().intValue();
            } while (keynum > transactioninsertkeysequence.lastValue());
        }
        return keynum;
    }
    @Override
    public boolean doInsert(DB db, Object threadstate) {
        String dbkey = Integer.toString(keysequence.nextValue().intValue());
        String dest = Integer.toString(keysequence.nextValue().intValue());
        HashMap<String, ByteIterator> values = new HashMap<>();
        values.put("destination", new StringByteIterator(dest));
        values.put("value", new StringByteIterator(Double.toString(ThreadLocalRandom.current().nextDouble(0, 20))));

        Status status;
        int numOfRetries = 0;
        do {
            status = db.insert(table, dbkey, values);
            if (null != status && status.isOk()) {
                break;
            }
            // Retry if configured. Without retrying, the load process will fail
            // even if one single insertion fails. User can optionally configure
            // an insertion retry limit (default is 0) to enable retry.
            if (++numOfRetries <= insertionRetryLimit) {
                System.err.println("Retrying insertion, retry count: " + numOfRetries);
                try {
                    // Sleep for a random number between [0.8, 1.2)*insertionRetryInterval.
                    int sleepTime = (int) (1000 * insertionRetryInterval * (0.8 + 0.4 * Math.random()));
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }

            } else {
                System.err.println("Error inserting, not retrying any more. number of attempts: " + numOfRetries +
                        "Insertion Retry Limit: " + insertionRetryLimit);
                break;

            }
        } while (true);

        return null != status && status.isOk();
    }
    @Override
    public void doTransactionRead(DB db) {
    // choose a random key
    String keyname = Integer.toString(nextKeynum());
    db.read(table, keyname, null, null);
}

    @Override
    public void doTransactionUpdate(DB db) {
        // choose a random key
        String keyname = Integer.toString(nextKeynum());
        db.update(table, keyname, null);
    }

    @Override
    public void doTransactionInsert(DB db) {
        // choose the next key
        int keynum = transactioninsertkeysequence.nextValue().intValue();

        try {
            String dbkey = Integer.toString(keynum);
            String dest = Integer.toString(transactioninsertkeysequence.nextValue().intValue());
            HashMap<String, ByteIterator> values = new HashMap<>();
            values.put("destination", new StringByteIterator(dest));
            values.put("value", new StringByteIterator(Double.toString(ThreadLocalRandom.current().nextDouble(0, 20))));
            db.insert(table, dbkey, values);
        } finally {
            transactioninsertkeysequence.acknowledge(keynum);
        }
    }
}
