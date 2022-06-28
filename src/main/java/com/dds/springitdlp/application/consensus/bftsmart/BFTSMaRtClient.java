package com.dds.springitdlp.application.consensus.bftsmart;

import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.core.messages.TOMMessageType;
import com.dds.springitdlp.application.consensus.PluggableConsensus;
import com.dds.springitdlp.application.results.TransactionResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@ConditionalOnProperty(name = "consensus.plane", havingValue = "bftsmart")
@Component
public class BFTSMaRtClient implements PluggableConsensus {
    AsynchServiceProxy serviceProxy;

    public BFTSMaRtClient() {
        this.serviceProxy = new AsynchServiceProxy(Integer.parseInt(System.getenv().get("REPLICA_ID")));
    }

    @Override
    public byte[] orderedOperation(byte[] command) {
        return this.serviceProxy.invokeOrdered(command);
    }

    @Override
    public byte[] unorderedOperation(byte[] command) {
        return this.serviceProxy.invokeUnordered(command);
    }

    @Override
    public void asyncOrderedOperation(byte[] command, CompletableFuture<List<TransactionResult>> future) {
        this.serviceProxy.invokeAsynchRequest(command, new BFTReplyHandler(this.serviceProxy, future), TOMMessageType.ORDERED_REQUEST);
    }

}
