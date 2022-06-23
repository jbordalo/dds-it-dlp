package com.dds.springitdlp.application.contracts;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BasicSmartContract implements SmartContract {

    @JsonIgnore
    private final long TIME_LIMIT = 24 * 60 * 60 * 1000;

    private String signature;

    @Override
    public TransactionResultStatus call(Transaction transaction) {
        System.out.println(this.getClass().getName());
        long timeElapsed = System.currentTimeMillis() - transaction.getTimestamp();

        if (transaction.getAmount() < 10.0 || timeElapsed > this.TIME_LIMIT) {
            return TransactionResultStatus.FAILED_TRANSACTION;
        } else {
            return TransactionResultStatus.OK_TRANSACTION;
        }
    }
}
