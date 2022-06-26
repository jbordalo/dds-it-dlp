package com.dds.springitdlp.application.consensus;

import com.dds.springitdlp.application.entities.results.TransactionResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public interface PluggableConsensus {
    long TIMEOUT = 10;

    byte[] orderedOperation(byte[] command);

    byte[] unorderedOperation(byte[] command);

    void asyncOrderedOperation(byte[] command, CompletableFuture<List<TransactionResult>> future);

    default long getDefaultTimeout() {
        return TIMEOUT;
    }
}