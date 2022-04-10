package com.dds.springitdlp.application.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class Transaction implements Serializable {
    private String origin;
    private String destination;
    private double amount;
}
