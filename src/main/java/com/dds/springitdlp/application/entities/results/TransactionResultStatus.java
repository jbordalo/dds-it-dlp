package com.dds.springitdlp.application.entities.results;

import java.io.Serializable;

public enum TransactionResultStatus implements Serializable {
    OK_TRANSACTION,
    REPEATED_TRANSACTION,
    FAILED_TRANSACTION
}
