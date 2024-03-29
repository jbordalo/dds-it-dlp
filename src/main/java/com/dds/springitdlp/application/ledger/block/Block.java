package com.dds.springitdlp.application.ledger.block;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.merkleTree.MerkleTree;
import com.dds.springitdlp.cryptography.Cryptography;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

@NoArgsConstructor
@Getter
public class Block implements Serializable {
    public static final int MIN_TRANSACTIONS_BLOCK = 8;
    private BlockHeader header;
    private ArrayList<Transaction> transactions;

    public Block(String previousHash, long difficulty, ArrayList<Transaction> transactions) {
        this.header = new BlockHeader(previousHash, MerkleTree.generateTree(transactions).getHash(), System.currentTimeMillis(), difficulty);
        this.transactions = transactions;
    }

    public static Block genesisBlock(Transaction rewardTransaction) {
        return new Block("", BlockHeader.DEFAULT_DIFFICULTY, new ArrayList<>(Collections.singletonList(rewardTransaction)));
    }

    /**
     * Auxiliary method to check the validity of a block
     *
     * @return true if hash is fine, false otherwise
     */
    public static boolean checkBlock(Block block) {
        String hash = Cryptography.hash(block.toString());
        return hash.startsWith("0".repeat((int) block.getHeader().getDifficulty()));
    }

    @Override
    public String toString() {
        return "Block{" +
                "header=" + header.toString() +
                ", transactions=" + transactions +
                '}';
    }
}
