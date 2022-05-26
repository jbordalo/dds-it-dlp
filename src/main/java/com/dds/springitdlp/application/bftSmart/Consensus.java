package com.dds.springitdlp.application.bftSmart;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public interface Consensus {
    void sendTransaction(Transaction transaction) throws ResponseStatusException;
    Ledger getLedger();
    double getBalance(Account account) throws ResponseStatusException;
    List<Transaction> getExtract(Account account);
    double getTotalValue(List<Account> accounts);
    double getGlobalLedgerValue();
}
