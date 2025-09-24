package com.IKov.auth_service.service.Impl;

import com.IKov.auth_service.entity.logs.LOG_LEVEL;
import com.IKov.auth_service.entity.tokens.TokenType;
import com.IKov.auth_service.service.LoginService;
import com.IKov.auth_service.service.props.ValidityProps;
import com.IKov.auth_service.web.dto.LoginRequest;
import com.IKov.auth_service.web.dto.TokenPair;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.interfaces.RSAPrivateKey;
import java.sql.Date;
import java.text.ParseException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginServiceImpl.class);
    private final RSAPrivateKey rsaPrivateKey;
    private final WebClient webClient;
    private final String rsaKeyId;
    private final ValidityProps validityProps;
    private final RedisTemplate<String, Object> redisTemplate;
    private final com.IKov.auth_service.service.Logger sendLogger;


    @Autowired
    public LoginServiceImpl(RSAPrivateKey rsaPrivateKey,
                            String rsaKeyId,
                            WebClient.Builder webClientBuilder,
                            ValidityProps validityProps,
                            RedisTemplate<String, Object> redisTemplate,
                            @Value("${url.user-service:http://user-service:80}") String baseUrl,
                            com.IKov.auth_service.service.Logger sendLogger) {
        this.rsaPrivateKey = rsaPrivateKey;
        this.rsaKeyId = rsaKeyId;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.validityProps = validityProps;
        this.redisTemplate = redisTemplate;
        this.sendLogger = sendLogger;
    }

    @Override
    public TokenPair generateNewTokenPair(String login, String password) {
        String logText = String.format("Attempting to generate a new token pair for user: %s", login);
        sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        LoginRequest loginRequest = new LoginRequest(login, password);
        Mono<Boolean> isDataCorrectMono = webClient
                .post()
                .uri("/validate")
                .bodyValue(loginRequest)
                .retrieve()
                .bodyToMono(Boolean.class);
        logText = String.format("Requesting validation from user-service for user: %s", login);
        sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));

        Boolean isDataCorrect = isDataCorrectMono.block();

        if (!Boolean.TRUE.equals(isDataCorrect)) {
            logger.warn("Validation failed for user: {}", login);
            throw new RuntimeException("Illegal data");
        }
        logText = String.format("User %s successfully validated.", login);
        sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));

        String accessToken = createToken(login, Long.parseLong(validityProps.getAccess()), TokenType.ACCESS);
        String refreshToken = createToken(login, Long.parseLong(validityProps.getRefresh()), TokenType.REFRESH);

        rememberRefreshToken(refreshToken, Long.parseLong(validityProps.getRefresh()));

        logText = String.format("Successfully generated and remembered tokens for user: %s", login);
        sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    public TokenPair renewTokenPair(String refreshToken) {
        String logText;
        sendLogger.addLog("Attempting to renew a token pair", LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        boolean isExists = validateRefreshToken(refreshToken);
        if(!isExists) {
            logText = String.format("Refresh token(%s) does not exist in Redis, renewal failed", refreshToken);
            sendLogger.addLog(logText, LOG_LEVEL.WARN, MDC.get("traceId"), MDC.get("userId"));
            throw new RuntimeException("Refresh token do not exist");
        }
        sendLogger.addLog("Refresh token exists in Redis. Proceeding with renewal.", LOG_LEVEL.WARN, MDC.get("traceId"), MDC.get("userId"));

        try{
            SignedJWT signedJWT = SignedJWT.parse(refreshToken);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            String login = claimsSet.getSubject();
            logText = String.format("Parsed refresh token for user: %s", login);
            sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));

            String accessToken = createToken(login, Long.parseLong(validityProps.getAccess()), TokenType.ACCESS);
            String refreshTokenNew = createToken(login, Long.parseLong(validityProps.getRefresh()), TokenType.REFRESH);

            forgetRefreshToken(refreshToken);
            rememberRefreshToken(refreshTokenNew, Long.parseLong(validityProps.getRefresh()));

            logText = String.format("Successfully renewed token pair for user: %s", login);
            sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
            return new TokenPair(accessToken, refreshToken);
        } catch (ParseException exception){
            sendLogger.addLog("Failed to parse refresh token", LOG_LEVEL.WARN, MDC.get("traceId"), MDC.get("userId"));
            throw new RuntimeException("Token can't be parsed");
        }

    }

    @Override
    public void logout(String token) {
        sendLogger.addLog("Logging out token", LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        forgetRefreshToken(token);
    }

    /**
     * Создание JWT токена с заданным сроком действия.
     */
    private String createToken(String subject, long validitySeconds, TokenType tokenType) {
        String logText;
        try {
            logText = String.format("Creating %s token for user: %s", tokenType, subject);
            sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
            JWSSigner signer = new RSASSASigner(rsaPrivateKey);
            Instant now = Instant.now();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer("http://auth-service")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(validitySeconds)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("token_type", tokenType.toString())
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKeyId)
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(signer);
            logText = String.format("%s token created successfully for user: %s", tokenType, subject);
            sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));

            return signedJWT.serialize();
        } catch (JOSEException exception) {
            sendLogger.addLog("Failed to create JWT token", LOG_LEVEL.WARN, MDC.get("traceId"), MDC.get("userId"));
            throw new RuntimeException("Failed to create JWT", exception);
        }
    }

    private void rememberRefreshToken(String refreshToken, Long ttl){
        String logText;
        logText = "Remembering refresh token in Redis.";

        sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        try {
            redisTemplate.opsForValue().set(refreshToken, "", ttl, TimeUnit.SECONDS);

            logText = String.format("Refresh token saved in Redis with TTL: %s seconds", ttl);
            sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        } catch (DataAccessException e) {
            logText = "Failed to save refresh token to Redis.";
            sendLogger.addLog(logText, LOG_LEVEL.WARN, MDC.get("traceId"), MDC.get("userId"));
            throw new RuntimeException("Не удалось сохранить ключ в Redis", e);
        }
    }

    private void forgetRefreshToken(String refreshToken){
        String logText = "Forgetting refresh token in Redis.";
        sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        try{
            redisTemplate.delete(refreshToken);
            logText = "Refresh token deleted from Redis.";
            sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));
        } catch (DataAccessException exception){
            logText = "Failed to delete refresh token from Redis.";
            sendLogger.addLog(logText, LOG_LEVEL.WARN, MDC.get("traceId"), MDC.get("userId"));
            throw new RuntimeException("Cannot delete refresh");
        }
    }

    private boolean validateRefreshToken(String refreshToken) {
        String logText;
        logText = "Validating refresh token existence in Redis.";
        sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));

        boolean isExists = redisTemplate.opsForValue().get(refreshToken) != null;

        logText = "Refresh token existence result: " + isExists;
        sendLogger.addLog(logText, LOG_LEVEL.DEBUG, MDC.get("traceId"), MDC.get("userId"));

        return isExists;
    }
}
