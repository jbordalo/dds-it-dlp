package com.dds.springitdlp.application.bftSmart;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ReplyHandler implements ReplyListener {
    private final AsynchServiceProxy serviceProxy;
    private final CompletableFuture<TransactionResult> future;
    private final Map<TransactionResult, Integer> replies = new ConcurrentHashMap<>();

    @Override
    public void reset() {
        this.replies.clear();
    }

    @Override
    public void replyReceived(RequestContext context, TOMMessage reply) {
        byte[] content = reply.getContent();

        TransactionResult transactionResult = getTransactionResult(content);

        this.replies.put(transactionResult, this.replies.getOrDefault(transactionResult, 0) + 1);

        int quorum = serviceProxy.getViewManager().getCurrentViewF() * 2 + 1;

        this.replies.forEach((result, replyCount) -> {
            if (replyCount >= quorum) {
                this.future.complete(result);
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
