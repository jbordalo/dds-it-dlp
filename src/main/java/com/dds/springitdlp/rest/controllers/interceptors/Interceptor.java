package com.dds.springitdlp.rest.controllers.interceptors;

import com.dds.springitdlp.application.entities.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.stream.Collectors;

@Component
public class Interceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String digitalSignature = request.getHeader("digitalSignature");

        String raw = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

        try {
            Signature signature = Signature.getInstance("SHA512withECDSA", "BC");

            Transaction trans = new ObjectMapper().readValue(raw, Transaction.class);

            String publicKeyPEM = trans.getOrigin().getAccountId();

            byte[] encoded = Base64.decodeBase64(publicKeyPEM);

            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey pkey = keyFactory.generatePublic(keySpec);

            signature.initVerify(pkey);

            signature.update(raw.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.decodeBase64(digitalSignature);
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | NoSuchProviderException |
                 InvalidKeySpecException e) {
            return false;
        }
    }

}