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
    public final static int SHA256_B64_LEN = 43;
    private String accountId;
    private String pubKey;

    public Account(String accountId) {
        this.accountId = accountId;
        if (!accountId.equals(SYSTEM))
            this.pubKey = parse(accountId);
    }

    public static Account SYSTEM_ACC() {
        return new Account(SYSTEM);
    }

    public static String parse(String accountId) {
        return accountId.substring(SHA256_B64_LEN);
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
