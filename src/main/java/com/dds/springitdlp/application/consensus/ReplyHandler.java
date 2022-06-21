package com.dds.springitdlp.application.consensus;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import com.dds.springitdlp.application.entities.results.TransactionResult;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ReplyHandler implements ReplyListener {
    private final AsynchServiceProxy serviceProxy;
    private final CompletableFuture<List<TransactionResult>> future;
    private final Map<TransactionResultStatus, LinkedList<TransactionResult>> replyCounter = new ConcurrentHashMap<>();

    @Override
    public void reset() {
        this.replyCounter.clear();
    }

    @Override
    public void replyReceived(RequestContext context, TOMMessage reply) {
        byte[] content = reply.getContent();

        TransactionResult transactionResult = getTransactionResult(content);

        if (!this.replyCounter.containsKey(transactionResult.getResult()))
            this.replyCounter.put(transactionResult.getResult(), new LinkedList<>());

        this.replyCounter.get(transactionResult.getResult()).add(transactionResult);

        int quorum = serviceProxy.getViewManager().getCurrentViewF() * 2 + 1;

        this.replyCounter.forEach((resultStatus, results) -> {
            if (results.size() >= quorum) {
                this.future.complete(replyCounter.get(resultStatus));
                this.serviceProxy.cleanAsynchRequest(context.getOperationId());
            }
        });
    }

    private TransactionResult getTransactionResult(byte[] content) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(content);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {
            return (TransactionResult) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
