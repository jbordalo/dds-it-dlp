package com.dds.springitdlp.application.entities;

import lombok.Data;

import java.io.Serializable;

@Data
public class Transaction implements Serializable {
    private String origin;
    private String destination;
    private double amount;
}
