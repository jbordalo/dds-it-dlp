package com.dds.springitdlp.rest.controllers.interceptors;
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
        if(digitalSignature != null) {
            String algorithm = request.getHeader("algorithm");
            String signedContent = request.getMethod() + " " +
                    request.getRequestURL().toString() + "?" + request.getQueryString()
                    + " " + algorithm;

            String publicKey = request.getParameter("accountId");
            try {

                Signature signature = Signature.getInstance(algorithm, "BC");
                byte[] encoded = Base64.decodeBase64(publicKey);
                KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
                PublicKey pkey = keyFactory.generatePublic(keySpec);
                signature.initVerify(pkey);
                signature.update(signedContent.getBytes(StandardCharsets.UTF_8));

                byte[] signatureBytes = Base64.decodeBase64(digitalSignature);
                return signature.verify(signatureBytes);
            } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | NoSuchProviderException |
                     InvalidKeySpecException e) {
                return false;
            }
        }
        return true;
    }

}