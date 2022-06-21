package com.dds.client;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.concurrent.Callable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SmartContract implements Serializable, Callable<TransactionResultStatus> {

    private final long TIME_LIMIT = 24 * 60 * 60 * 1000;
    Transaction transaction;

    @Override
    public TransactionResultStatus call() {
        System.out.println("Endorser: running smart contract");

        long timeElapsed = System.currentTimeMillis() - this.transaction.getTimestamp();

        if (this.transaction.getAmount() < 10.0 || timeElapsed > this.TIME_LIMIT) {
            return TransactionResultStatus.FAILED_TRANSACTION;
        } else {
            return TransactionResultStatus.OK_TRANSACTION;
        }
    }
}
