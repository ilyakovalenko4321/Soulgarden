package com.IKov.auth_service.service;

import com.IKov.auth_service.entity.logs.LOG_LEVEL;
import reactor.core.publisher.Mono;


public interface Logger {

    Mono<Void> addLog(String log, LOG_LEVEL logLevel, String traceId, String userId);

    Mono<String> addLogAndGetIt(String log, LOG_LEVEL logLevel, String traceId, String userId);

}
