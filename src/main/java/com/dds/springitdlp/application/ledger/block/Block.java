package com.dds.springitdlp.application.ledger.block;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.merkleTree.MerkleTree;
import lombok.Data;

import java.util.ArrayList;

@Data
public class Block {
    private static final int TRANSACTION_COUNT = 12;
    private final BlockHeader header;
    private final ArrayList<Transaction> transactions;

    public Block(String previousHash, long difficulty, ArrayList<Transaction> transactions) {
        this.header = new BlockHeader(previousHash, MerkleTree.generateTree(transactions), System.currentTimeMillis(), difficulty);
        this.transactions = transactions;
    }

}
