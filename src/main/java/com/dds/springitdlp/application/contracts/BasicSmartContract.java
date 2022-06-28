package com.dds.springitdlp.application.contracts;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.results.TransactionResultStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BasicSmartContract implements SmartContract {
    private String uuid;
    private String endorserId;
    private String signature;

    @Override
    public TransactionResultStatus call(Transaction transaction) {
        if (transaction.getAmount() < 10.0) {
            return TransactionResultStatus.FAILED_TRANSACTION;
        } else {
            return TransactionResultStatus.OK_TRANSACTION;
        }
    }
}
