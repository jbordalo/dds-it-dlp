package com.dds.springitdlp.application.contracts;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;

import java.io.Serializable;

public interface SmartContract extends Serializable {
    String getUuid();

    void setUuid(String uuid);

    String getEndorserId();

    default String serialize() {
        return this.getUuid() + this.getEndorserId();
    }

    void setEndorserId(String endorserId);

    String getSignature();

    void setSignature(String signature);

    TransactionResultStatus call(Transaction transaction);
}
