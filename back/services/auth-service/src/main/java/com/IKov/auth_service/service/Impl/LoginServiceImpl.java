package com.IKov.auth_service.service.Impl;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.interfaces.RSAPrivateKey;
import java.sql.Date;
import java.text.ParseException;
import java.time.Instant;
import java.util.Objects;
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


    @Autowired
    public LoginServiceImpl(RSAPrivateKey rsaPrivateKey,
                            String rsaKeyId,
                            WebClient.Builder webClientBuilder,
                            ValidityProps validityProps,
                            RedisTemplate<String, Object> redisTemplate) {
        this.rsaPrivateKey = rsaPrivateKey;
        this.rsaKeyId = rsaKeyId;
        this.webClient = webClientBuilder.baseUrl("http://user-service").build();
        this.validityProps = validityProps;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public TokenPair generateNewTokenPair(String login, String password) {
        logger.info("Attempting to generate a new token pair for user: {}", login);
        LoginRequest loginRequest = new LoginRequest(login, password);
        //Test
        Mono<Boolean> isDataCorrectMono = webClient
                .post()
                .uri("/validate")
                .bodyValue(loginRequest)
                .retrieve()
                .bodyToMono(Boolean.class);
        logger.debug("Requesting validation from user-service for user: {}", login);

        Boolean isDataCorrect = isDataCorrectMono.block();

        if (!Boolean.TRUE.equals(isDataCorrect)) {
            logger.warn("Validation failed for user: {}", login);
            throw new RuntimeException("Illegal data");
        }
        logger.info("User {} successfully validated.", login);

        String accessToken = createToken(login, Long.parseLong(validityProps.getAccess()), TokenType.ACCESS);
        String refreshToken = createToken(login, Long.parseLong(validityProps.getRefresh()), TokenType.REFRESH);

        rememberRefreshToken(refreshToken, Long.parseLong(validityProps.getRefresh()));

        logger.info("Successfully generated and remembered tokens for user: {}", login);
        return new TokenPair(accessToken, refreshToken);
    }

    @Override
    public TokenPair renewTokenPair(String refreshToken) {
        logger.info("Attempting to renew a token pair.");
        boolean isExists = validateRefreshToken(refreshToken);
        if(!isExists) {
            logger.warn("Refresh token does not exist in Redis, renewal failed.");
            throw new RuntimeException("Refresh token do not exist");
        }
        logger.debug("Refresh token exists in Redis. Proceeding with renewal.");

        try{
            SignedJWT signedJWT = SignedJWT.parse(refreshToken);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            String login = claimsSet.getSubject();
            logger.info("Parsed refresh token for user: {}", login);

            String accessToken = createToken(login, Long.parseLong(validityProps.getAccess()), TokenType.ACCESS);
            String refreshTokenNew = createToken(login, Long.parseLong(validityProps.getRefresh()), TokenType.REFRESH);

            forgetRefreshToken(refreshToken);
            rememberRefreshToken(refreshTokenNew, Long.parseLong(validityProps.getRefresh()));

            logger.info("Successfully renewed token pair for user: {}", login);
            return new TokenPair(accessToken, refreshToken);
        } catch (ParseException exception){
            logger.error("Failed to parse refresh token.", exception);
            throw new RuntimeException("Token can't be parsed");
        }

    }

    @Override
    public void logout(String token) {
        logger.info("Logging out token.");
        forgetRefreshToken(token);
    }

    /**
     * Создание JWT токена с заданным сроком действия.
     */
    private String createToken(String subject, long validitySeconds, TokenType tokenType) {
        try {
            logger.debug("Creating {} token for user: {}", tokenType, subject);
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
            logger.debug("{} token created successfully for user: {}", tokenType, subject);

            return signedJWT.serialize();
        } catch (JOSEException exception) {
            logger.error("Failed to create JWT token.", exception);
            throw new RuntimeException("Failed to create JWT", exception);
        }
    }

    private void rememberRefreshToken(String refreshToken, Long ttl){
        logger.debug("Remembering refresh token in Redis.");
        try {
            redisTemplate.opsForValue().set(refreshToken, "", ttl, TimeUnit.SECONDS);
            logger.debug("Refresh token saved in Redis with TTL: {} seconds", ttl);
        } catch (DataAccessException e) {
            logger.error("Failed to save refresh token to Redis.", e);
            throw new RuntimeException("Не удалось сохранить ключ в Redis", e);
        }
    }

    private void forgetRefreshToken(String refreshToken){
        logger.debug("Forgetting refresh token in Redis.");
        try{
            redisTemplate.delete(refreshToken);
            logger.debug("Refresh token deleted from Redis.");
        } catch (DataAccessException exception){
            logger.error("Failed to delete refresh token from Redis.", exception);
            throw new RuntimeException("Cannot delete refresh");
        }
    }

    private boolean validateRefreshToken(String refreshToken){
        logger.debug("Validating refresh token existence in Redis.");
        boolean isExists = redisTemplate.opsForValue().get(refreshToken)!=null;
        logger.debug("Refresh token existence result: {}", isExists);
        return isExists;
    }
}
