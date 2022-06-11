package com.dds.springitdlp.application.ledger.block;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.merkleTree.MerkleTree;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

@Data
public class Block implements Serializable {
    public static final int MIN_TRANSACTIONS_BLOCK = 12;
    private final BlockHeader header;
    private final ArrayList<Transaction> transactions;

    public Block(String previousHash, long difficulty, long timestamp, ArrayList<Transaction> transactions) {
        this.header = new BlockHeader(previousHash, MerkleTree.generateTree(transactions), timestamp, difficulty);
        this.transactions = transactions;
    }

    public Block(String previousHash, long difficulty, ArrayList<Transaction> transactions) {
        this(previousHash, difficulty, System.currentTimeMillis(), transactions);
    }

    public static Block genesisBlock() {
        return new Block("", 1, 0, new ArrayList<>(Collections.singletonList(new Transaction())));
    }
}
