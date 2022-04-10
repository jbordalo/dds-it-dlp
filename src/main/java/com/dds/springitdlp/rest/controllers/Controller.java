package com.dds.springitdlp.rest.controllers;

import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.services.AppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/getLedger")
    public Ledger getLedger() {
        return service.getLedger();
    }

}
