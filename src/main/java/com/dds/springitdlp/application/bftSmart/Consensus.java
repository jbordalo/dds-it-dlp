package com.dds.springitdlp.application.bftSmart;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.TransactionResult;
import com.dds.springitdlp.application.ledger.block.Block;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public interface Consensus {
    TransactionResult sendTransaction(Transaction transaction) throws ResponseStatusException;

    TransactionResult sendAsyncTransaction(Transaction transaction) throws ResponseStatusException;

    Ledger getLedger();

    double getBalance(Account account) throws ResponseStatusException;

    List<Transaction> getExtract(Account account);

    double getTotalValue(List<Account> accounts);

    double getGlobalLedgerValue();

    boolean proposeBlock(Block block);
}
