package com.dds.springitdlp.rest.controllers;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.ledger.Ledger;
import com.dds.springitdlp.application.ledger.block.Block;
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
            this.service.sendTransaction(transaction);
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/sendAsyncTransaction")
    public void sendAsyncTransaction(@RequestParam String accountId, @RequestBody Transaction transaction) {
        if (transaction.getOrigin().getAccountId().equals(accountId)) {
            this.service.sendAsyncTransaction(transaction);
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
        return service.getLedger();
    }

    @GetMapping("/block")
    public Block getBlockToMine() { return null; }
    @PostMapping("/propose")
    public boolean proposeBlock(@RequestBody Block block) { return false; }

}
