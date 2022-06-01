package com.dds.springitdlp.application.ledger.block;

import com.dds.springitdlp.application.ledger.merkleTree.Node;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class BlockHeader {
    private final byte version = 0x01;
    private final String previousHash;
    private final Node merkleRoot;
    private final long timestamp;
    private final long difficulty;
    private int nonce;
}
