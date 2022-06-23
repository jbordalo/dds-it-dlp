package com.dds.springitdlp.application.contracts;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;

import java.io.Serializable;

public interface SmartContract extends Serializable {
    String getUuid();

    void setUuid(String uuid);

    String getSignature();

    void setSignature(String signature);

    TransactionResultStatus call(Transaction transaction);
}
