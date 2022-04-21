package com.dds.springitdlp.rest.controllers;

import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.services.AppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/")
public class Controller {
    private final AppService service;

    @Autowired
    public Controller(AppService service) {
        this.service = service;
    }

    @PostMapping("/sendTransaction")
    public void sendTransaction(@RequestParam String accountId, @RequestBody Transaction transaction) {
        if (transaction.getOrigin().getAccountId().equals(accountId))
            this.service.sendTransaction(transaction);
        //TODO ?? exception?
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

}
