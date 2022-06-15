package com.dds.springitdlp.rest.controllers.interceptors;

import com.dds.springitdlp.application.entities.Account;
import com.dds.springitdlp.cryptography.Cryptography;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

@Component
public class Interceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String digitalSignature = request.getHeader("signature");
        if (digitalSignature != null) {
            String algorithm = request.getHeader("algorithm");
            String signedContent = request.getMethod() + " " +
                    request.getRequestURL().toString() + "?" + request.getQueryString()
                    + " " + algorithm;

            String publicKey = Account.parse(request.getParameter("accountId"));
            return Cryptography.verify(publicKey, signedContent, digitalSignature);
        }
        return true;
    }

}