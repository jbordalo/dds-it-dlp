package com.dds.springitdlp.rest.controllers;

import com.dds.springitdlp.application.contracts.Endorser;
import com.dds.springitdlp.application.contracts.SmartContract;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.naming.NoPermissionException;
import java.io.*;
import java.security.Security;

@RestController
@RequestMapping("/")
@ConditionalOnProperty(name = "endorser")
public class EndorserController {
    private final Endorser endorser;

    @Autowired
    public EndorserController(Endorser endorser) {
        Security.addProvider(new BouncyCastleProvider());
        this.endorser = endorser;
    }

    @PostMapping("/endorse")
    public byte[] endorse(@RequestBody byte[] smartContract) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(smartContract);
             ObjectInput objIn = new ObjectInputStream(byteIn)) {

            SmartContract contract = (SmartContract) objIn.readObject();

            contract = this.endorser.endorse(contract);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(contract);
                oos.flush();

                return bos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (NoPermissionException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (OutOfMemoryError e) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return null;
    }
}
