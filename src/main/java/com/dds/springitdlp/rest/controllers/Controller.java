package com.dds.springitdlp.rest.controllers;

import com.dds.springitdlp.application.entities.Ledger;
import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.services.AppService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.*;
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

    @GetMapping("/totalValue")
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
