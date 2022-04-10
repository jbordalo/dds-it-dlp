package com.dds.springitdlp.application.entities;

import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Ledger implements Serializable {
    @Getter
    private Map<Account, List<Transaction>> map;

    public Ledger() {
        map = new HashMap<>();
    }
}
