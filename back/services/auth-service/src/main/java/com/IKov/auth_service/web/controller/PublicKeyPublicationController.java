package com.IKov.auth_service.web.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

@RestController
@RequestMapping("/publicate")
public class PublicKeyPublicationController {

    private static final Logger logger = LoggerFactory.getLogger(PublicKeyPublicationController.class);
    private final RSAPublicKey publicKey;
    private final String rsaKeyId;

    public PublicKeyPublicationController(RSAPublicKey publicKey, @Qualifier("rsaKeyId") String rsaKeyId) {
        this.publicKey = publicKey;
        this.rsaKeyId = rsaKeyId;
    }

    @GetMapping(".well-known/jwks.json")
    public Map<String, Object> keys(){
        logger.info("Serving JWKS public key.");
        RSAKey jwk = new RSAKey.Builder(publicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(rsaKeyId)
                .build();
        JWKSet set = new JWKSet(jwk);
        return set.toJSONObject();
    }

}
