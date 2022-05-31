package com.dds.springitdlp.application.bftSmart;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.RequestContext;
import bftsmart.tom.core.messages.TOMMessage;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ReplyHandler implements ReplyListener {

    private final AsynchServiceProxy serviceProxy;
    private final CompletableFuture<byte[]> future;
    private final Map<ReplyWrapper, Integer> replies = new ConcurrentHashMap<>();

    @Override
    public void reset() {
        this.replies.clear();
    }

    @Override
    public void replyReceived(RequestContext context, TOMMessage reply) {
        byte[] content = reply.getContent();
        ReplyWrapper replyWrapper = new ReplyWrapper(content);

        this.replies.put(replyWrapper, this.replies.getOrDefault(replyWrapper, 0) + 1);

        int quorum = serviceProxy.getViewManager().getCurrentViewF() * 2 + 1;

        this.replies.forEach((replyBytes, replyCount) -> {
            if (replyCount >= quorum) {
                this.future.complete(replyBytes.getReply());
                this.serviceProxy.cleanAsynchRequest(context.getOperationId());
            }
        });
    }
}
