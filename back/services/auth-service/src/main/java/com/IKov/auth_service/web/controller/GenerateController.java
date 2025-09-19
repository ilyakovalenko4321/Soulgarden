package com.IKov.auth_service.web.controller;

import com.IKov.auth_service.service.LoginService;
import com.IKov.auth_service.web.dto.LoginRequest;
import com.IKov.auth_service.web.dto.TokenPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/generate")
public class GenerateController {

    private static final Logger logger = LoggerFactory.getLogger(GenerateController.class);
    private final LoginService loginService;

    public GenerateController(LoginService loginService){
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public TokenPair generateTokenPair(@RequestBody @Validated LoginRequest loginRequest){
        logger.info("Received login request for user: {}", loginRequest.getLogin());

        TokenPair tokenPair = loginService.generateNewTokenPair(loginRequest.getLogin(), loginRequest.getPassword());
        logger.info("Successfully generated new token pair for user: {}", loginRequest.getLogin());
        return tokenPair;
    }

    @GetMapping("/refresh")
    public TokenPair refreshToken(String refreshToken){
        logger.info("Received refresh token request.");
        TokenPair tokenPair = loginService.renewTokenPair(refreshToken);
        logger.info("Successfully refreshed token pair.");
        return tokenPair;
    }

}
