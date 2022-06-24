package com.dds.springitdlp.application.contracts;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class BasicSmartContract implements SmartContract {
    private final long TIME_LIMIT = 24 * 60 * 60 * 1000;
    private String uuid;
    private String endorserId;

    @Getter
    @Setter
    private String signature;

    @Override
    public String getUuid() {
        return this.uuid;
    }

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getEndorserId() {
        return this.endorserId;
    }

    @Override
    public void setEndorserId(String endorserId) {
        this.endorserId = endorserId;
    }

    @Override
    public TransactionResultStatus call(Transaction transaction) {
        long timeElapsed = System.currentTimeMillis() - transaction.getTimestamp();

        if (transaction.getAmount() < 10.0 || timeElapsed > this.TIME_LIMIT) {
            return TransactionResultStatus.FAILED_TRANSACTION;
        } else {
            return TransactionResultStatus.OK_TRANSACTION;
        }
    }
}
