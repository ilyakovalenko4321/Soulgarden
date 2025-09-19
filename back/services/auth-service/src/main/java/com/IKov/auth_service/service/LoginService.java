package com.IKov.auth_service.service;

import com.IKov.auth_service.web.dto.TokenPair;

public interface LoginService {

    TokenPair generateNewTokenPair(String login, String password);

    TokenPair renewTokenPair(String refreshToken);

    void logout(String token);

}
