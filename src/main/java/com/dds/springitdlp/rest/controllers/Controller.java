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
    public int sendTransaction(@RequestBody Transaction transaction) {
        return service.sendTransaction(transaction);
    }

    @GetMapping("/balance/{accountId}")
    public double getBalance(@PathVariable String accountId) {
        return this.service.getBalance(accountId);
    }

    @GetMapping("/extract/{accountId}")
    public List<Transaction> getExtract(@PathVariable String accountId) {
        return this.service.getExtract(accountId);
    }

    @GetMapping("/totalValue")
    public double getTotalValue(@RequestBody List<String> accounts) {
        return this.service.getTotalValue(accounts);
    }

    @GetMapping("/ledger")
    public Ledger getLedger() {
        return service.getLedger();
    }

}
