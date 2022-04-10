package com.dds.springitdlp.application.entities;

import lombok.Data;

@Data
public class Transaction {
    private String origin;
    private String destination;
    private double amount;
}
