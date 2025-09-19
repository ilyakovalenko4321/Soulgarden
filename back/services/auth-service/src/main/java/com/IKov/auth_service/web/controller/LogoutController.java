package com.IKov.auth_service.web.controller;

import com.IKov.auth_service.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logout")
public class LogoutController {

    private final LoginService loginService;

    public LogoutController(LoginService loginService){
        this.loginService = loginService;
    }

    @GetMapping("/logout")
    public boolean logout(String refreshToken){
        loginService.logout(refreshToken);
        return true;
    }

}
