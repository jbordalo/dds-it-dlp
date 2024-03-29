package com.dds.springitdlp.application.ledger.block;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@NoArgsConstructor
public class BlockHeader implements Serializable {
    public static final long DEFAULT_DIFFICULTY = 3;
    @SuppressWarnings("FieldMayBeFinal")
    private byte version = 0x01;
    private String previousHash;
    private String merkleRoot;
    private long timestamp;
    private long difficulty;
    @Setter
    private int nonce;

    public BlockHeader(String previousHash, String merkleRoot, long timestamp, long difficulty) {
        this.previousHash = previousHash;
        this.merkleRoot = merkleRoot;
        this.timestamp = timestamp;
        this.difficulty = difficulty;
    }

    @Override
    public String toString() {
        return "BlockHeader{" +
                "version=" + version +
                ", previousHash='" + previousHash + '\'' +
                ", merkleRoot=" + merkleRoot +
                ", timestamp=" + timestamp +
                ", difficulty=" + difficulty +
                ", nonce=" + nonce +
                '}';
    }
}
