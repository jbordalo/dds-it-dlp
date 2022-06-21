package com.dds.springitdlp.dataPlane;

import com.dds.springitdlp.application.entities.Transaction;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@RedisHash("TransactionPool")
@Getter
public class TransactionPool implements Serializable {
    private final List<Transaction> transactionPool;

    @Id
    private String id = System.getenv("REPLICA_ID");

    public TransactionPool() {
        this.transactionPool = new LinkedList<>();
    }

}
