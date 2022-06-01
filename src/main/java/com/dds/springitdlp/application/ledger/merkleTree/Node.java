package com.dds.springitdlp.application.ledger.merkleTree;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Node {
    private Node left;
    private Node right;
    private String hash;
}