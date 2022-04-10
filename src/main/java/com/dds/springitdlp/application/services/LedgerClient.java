package com.dds.springitdlp.application.services;

import bftsmart.tom.ServiceProxy;

public class LedgerClient {

    ServiceProxy serviceProxy;

    public LedgerClient(int id) {
        serviceProxy = new ServiceProxy(id);
    }
}
