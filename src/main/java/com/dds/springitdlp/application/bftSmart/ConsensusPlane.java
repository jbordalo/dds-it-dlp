package com.dds.springitdlp.application.bftSmart;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.ProposeResult;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.block.Block;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public interface ConsensusPlane {
    TransactionResult sendTransaction(Transaction transaction) throws ResponseStatusException;

    TransactionResult sendAsyncTransaction(Transaction transaction) throws ResponseStatusException;

    Ledger getLedger();

    double getBalance(Account account) throws ResponseStatusException;

    List<Transaction> getExtract(Account account);

    double getTotalValue(List<Account> accounts);

    double getGlobalLedgerValue();

    ProposeResult proposeBlock(Block block);
}
