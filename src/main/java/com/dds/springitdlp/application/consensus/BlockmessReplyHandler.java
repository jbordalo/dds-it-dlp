package com.dds.springitdlp.application.consensus;

import applicationInterface.ReplyListener;
import com.dds.springitdlp.application.entities.results.TransactionResult;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

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
public class BlockmessReplyHandler implements ReplyListener {
    private final CompletableFuture<List<TransactionResult>> future;
    private final Map<TransactionResultStatus, LinkedList<TransactionResult>> replyCounter = new ConcurrentHashMap<>();

    private TransactionResult getTransactionResult(byte[] content) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(content);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {
            return (TransactionResult) objIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processReply(Pair<byte[], Long> pair) {
        byte[] content = pair.getLeft();

        TransactionResult transactionResult = getTransactionResult(content);

        if (!this.replyCounter.containsKey(transactionResult.getResult()))
            this.replyCounter.put(transactionResult.getResult(), new LinkedList<>());

        this.replyCounter.get(transactionResult.getResult()).add(transactionResult);

        // TODO
        int quorum = 3;

        this.replyCounter.forEach((resultStatus, results) -> {
            if (results.size() >= quorum) {
                this.future.complete(replyCounter.get(resultStatus));
            }
        });
    }
}
