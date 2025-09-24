package com.IKov.auth_service.web.controller;

import com.IKov.auth_service.entity.logs.LOG_LEVEL;
import com.IKov.auth_service.service.Logger;
import com.IKov.auth_service.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logout")
public class LogoutController {

    private final LoginService loginService;
    private final Logger logger;

    public LogoutController(LoginService loginService,
                            Logger logger){
        this.loginService = loginService;
        this.logger = logger;
    }

    @GetMapping("/logout")
    public boolean logout(String refreshToken){
        logger.addLog("Logout user", LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        loginService.logout(refreshToken);
        return true;
    }

}
