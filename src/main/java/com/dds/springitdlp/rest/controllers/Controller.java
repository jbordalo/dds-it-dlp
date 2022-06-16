package com.dds.springitdlp.rest.controllers;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.entities.results.ProposeResult;
import com.dds.springitdlp.application.entities.results.TransactionResultStatus;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.block.Block;
import com.dds.springitdlp.application.ledger.block.BlockRequest;
import com.dds.springitdlp.application.services.AppService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Security;
import java.util.List;

@RestController
@RequestMapping("/")
public class Controller {
    private final AppService service;

    @Autowired
    public Controller(AppService service) {
        Security.addProvider(new BouncyCastleProvider());
        this.service = service;
    }

    @PostMapping("/sendTransaction")
    public void sendTransaction(@RequestParam String accountId, @RequestBody Transaction transaction) {
        if (transaction.getOrigin().getAccountId().equals(accountId)) {
            TransactionResultStatus result = this.service.sendTransaction(transaction);
            if (result == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            if (result == TransactionResultStatus.FAILED_TRANSACTION)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            if (result == TransactionResultStatus.REPEATED_TRANSACTION)
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/sendAsyncTransaction")
    public void sendAsyncTransaction(@RequestParam String accountId, @RequestBody Transaction transaction) {
        if (transaction.getOrigin().getAccountId().equals(accountId)) {
            TransactionResultStatus result = this.service.sendAsyncTransaction(transaction);
            if (result == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            if (result == TransactionResultStatus.FAILED_TRANSACTION)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            if (result == TransactionResultStatus.REPEATED_TRANSACTION)
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/balance")
    public double getBalance(@RequestParam String accountId) {
        return this.service.getBalance(accountId);
    }

    @GetMapping("/extract")
    public List<Transaction> getExtract(@RequestParam String accountId) {
        return this.service.getExtract(accountId);
    }

    @PostMapping("/totalValue")
    public double getTotalValue(@RequestBody List<String> accounts) {
        return this.service.getTotalValue(accounts);
    }

    @GetMapping("/globalLedgerValue")
    public double getTotalValue() {
        return this.service.getGlobalLedgerValue();
    }

    @GetMapping("/ledger")
    public Ledger getLedger() {
        return this.service.getLedger();
    }

    @PostMapping("/block")
    public Block getBlockToMine(@RequestBody BlockRequest blockRequest) {
        Block block = this.service.getBlock(blockRequest);
        if (block == null) throw new ResponseStatusException(HttpStatus.NO_CONTENT);

        return block;
    }

    @PostMapping("/proposeBlock")
    public void proposeBlock(@RequestBody Block block) {
        ProposeResult result = this.service.proposeBlock(block);

        if (result == ProposeResult.BLOCK_REJECTED) throw new ResponseStatusException(HttpStatus.CONFLICT);
    }

}
