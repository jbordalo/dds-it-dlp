package com.dds.springitdlp.application.services;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;

import java.util.logging.Logger;

public class LedgerServer extends DefaultSingleRecoverable {
    private final Logger logger;

    public LedgerServer(int id) {
        logger = Logger.getLogger(LedgerServer.class.getName());
        new ServiceReplica(id, this, this);
    }

    @Override
    public void installSnapshot(byte[] bytes) {
    }

    @Override
    public byte[] getSnapshot() {
        return new byte[0];
    }

    @Override
    public byte[] appExecuteOrdered(byte[] bytes, MessageContext messageContext) {
        return new byte[0];
    }

    @Override
    public byte[] appExecuteUnordered(byte[] bytes, MessageContext messageContext) {
        return new byte[0];
    }
}
