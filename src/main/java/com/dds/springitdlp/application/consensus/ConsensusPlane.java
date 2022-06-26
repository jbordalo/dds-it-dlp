package com.dds.springitdlp.application.consensus;

import com.dds.springitdlp.application.contracts.SmartContract;
import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.ProposeResult;
import com.dds.springitdlp.application.entities.results.RegisterResult;
import com.dds.springitdlp.application.entities.results.TransactionResult;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.block.Block;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public interface ConsensusPlane {
    TransactionResult sendTransaction(Transaction transaction) throws ResponseStatusException;

    List<TransactionResult> sendAsyncTransaction(Transaction transaction) throws ResponseStatusException;

    Ledger getLedger();

    double getBalance(Account account) throws ResponseStatusException;

    List<Transaction> getExtract(Account account);

    double getTotalValue(List<Account> accounts);

    double getGlobalLedgerValue();

    ProposeResult proposeBlock(Block block);

    RegisterResult registerSmartContract(SmartContract contract);
}