package com.dds.springitdlp.application.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction implements Serializable {

    public static final double INITIAL_VALUE = 100.0;

    private Account origin;
    private Account destination;
    private double amount;

    public static Transaction SYS_INIT(Account account) {
        return new Transaction(Account.SYSTEM_ACC(), account, Transaction.INITIAL_VALUE);
    }
}
