package com.dds.springitdlp.application.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Account implements Serializable {
    public final static String SYSTEM = "GOD";
    private  String accountId;
    private  String ownerId;

    public Account(String accountId) {
        this.accountId = accountId;
        // TODO parse
        this.ownerId = accountId;
    }

    public static Account SYSTEM_ACC() {
        return new Account(SYSTEM);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return accountId.equals(account.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }
}
