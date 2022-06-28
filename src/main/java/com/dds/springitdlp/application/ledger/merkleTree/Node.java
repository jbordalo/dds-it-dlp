package com.dds.springitdlp.application.ledger.merkleTree;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Node implements Serializable {
    private Node left;
    private Node right;
    private String hash;
}