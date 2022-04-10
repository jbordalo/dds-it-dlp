package com.dds.springitdlp.rest.controllers;

import com.dds.springitdlp.application.entities.Transaction;
import com.dds.springitdlp.application.services.AppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
