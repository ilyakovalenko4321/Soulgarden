package com.IKov.auth_service.web.controller;

import com.IKov.auth_service.entity.logs.LOG_LEVEL;
import com.IKov.auth_service.service.LoginService;
import com.IKov.auth_service.web.dto.LoginRequest;
import com.IKov.auth_service.web.dto.TokenPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/generate")
public class GenerateController {

    private static final Logger logger = LoggerFactory.getLogger(GenerateController.class);
    private final com.IKov.auth_service.service.Logger loggerSend;
    private final LoginService loginService;

    public GenerateController(LoginService loginService,
                              com.IKov.auth_service.service.Logger loggerSend){
        this.loginService = loginService;
        this.loggerSend = loggerSend;
    }

    @PostMapping("/login")
    public TokenPair generateTokenPair(@RequestBody @Validated LoginRequest loginRequest){
        String logText = String.format("Received login request for user: %s", loginRequest.getLogin());
        loggerSend.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));

        TokenPair tokenPair = loginService.generateNewTokenPair(loginRequest.getLogin(), loginRequest.getPassword());

        logText = String.format("Successfully generated new token pair for user: %s", loginRequest.getLogin());
        loggerSend.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        return tokenPair;
    }

    @GetMapping("/refresh")
    public TokenPair refreshToken(String refreshToken){
        loggerSend.addLog("Received refresh token request", LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        TokenPair tokenPair = loginService.renewTokenPair(refreshToken);
        loggerSend.addLog("Successfully refreshed token pair", LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        return tokenPair;
    }

}
