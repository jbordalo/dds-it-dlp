package com.dds.springitdlp.application.consensus.blockmess;

import applicationInterface.ApplicationInterface;
import com.dds.springitdlp.application.consensus.PluggableConsensus;
import com.dds.springitdlp.application.entities.results.TransactionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@ConditionalOnProperty(name = "consensus.plane", havingValue = "blockmess")
@Component
public class BlockmessClient implements PluggableConsensus {
    ApplicationInterface applicationInterface;

    @Autowired
    public BlockmessClient(ApplicationInterface applicationInterface) {
        this.applicationInterface = applicationInterface;
    }

    @Override
    public byte[] orderedOperation(byte[] command) {
        return this.applicationInterface.invokeSyncOperation(command).getLeft();
    }

    @Override
    public byte[] unorderedOperation(byte[] command) {
        return this.applicationInterface.invokeSyncOperation(command).getLeft();
    }

    @Override
    public void asyncOrderedOperation(byte[] command, CompletableFuture<List<TransactionResult>> future) {
        this.applicationInterface.invokeAsyncOperation(command, new BlockmessReplyHandler(future));
    }
}
