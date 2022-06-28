package com.dds.springitdlp.application.consensus.blockmess;

import applicationInterface.ApplicationInterface;
import com.dds.springitdlp.application.consensus.ConsensusServer;
import com.dds.springitdlp.application.ledger.LedgerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@ConditionalOnProperty(name = "blockmess.enabled")
@Component
public class BlockmessServer extends ApplicationInterface {
    private final Logger logger = Logger.getLogger(BlockmessServer.class.getName());
    private final LedgerHandler ledgerHandler;

    @Autowired
    public BlockmessServer(LedgerHandler ledgerHandler) {
        super(new String[]{"address=" + System.getenv("IP"), "port=" + System.getenv("BLOCKMESS_PORT")});
        this.ledgerHandler = ledgerHandler;
    }

    @Override
    public byte[] processOperation(byte[] command) {
        return ConsensusServer.executeOrderedOperation(command, this.ledgerHandler, this.logger);
    }
}
