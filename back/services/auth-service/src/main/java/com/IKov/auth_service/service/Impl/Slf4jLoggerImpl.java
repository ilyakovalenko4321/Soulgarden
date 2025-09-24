package com.IKov.auth_service.service.Impl;

import com.IKov.auth_service.entity.health.Health;
import com.IKov.auth_service.entity.logs.LOG_LEVEL;
import com.IKov.auth_service.entity.logs.LogEvent;
import com.IKov.auth_service.service.LogShipper;
import com.IKov.auth_service.service.Logger;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;

@Service
public class Slf4jLoggerImpl implements Logger {

    private final Health health;
    private final Sinks.Many<LogEvent> logEvents;
    private final LogShipper logShipper;
    private final String podId;

    public Slf4jLoggerImpl(
            String podId,
            LogShipper logShipper,
            Health health
    ){
        this.podId = podId;
        this.logShipper = logShipper;
        this.logEvents = Sinks.many().multicast().onBackpressureBuffer();
        this.health = health;
    }

    @PostConstruct
    private void init() {
        logEvents.asFlux()
                .buffer(Duration.ofSeconds(20))
                .flatMap(logs -> logShipper.sendBatch(logs)
                        .doOnError(error -> health.setIsLogNetworkReady(false))
                        .retryWhen(Retry.fixedDelay(9, Duration.ofSeconds(10))
                                .onRetryExhaustedThrow(((retryBackoffSpec, retrySignal) -> {
                                    health.setIsContainerLive(false);
                                    throw new RuntimeException("Failed to send logs to collector after multipyRetries", retrySignal.failure());
                                })))
                        .doOnSuccess(
                                result -> {
                                    health.setIsLogNetworkReady(true);
                                    result.ifPresent(failed -> {
                                        Flux.fromIterable(failed)
                                                .doFirst(() -> health.setIsLogNetworkReady(false))
                                                .cast(LogEvent.class)
                                                .doOnNext(logEvents::tryEmitNext)
                                                .subscribe();
                                    });
                                }
                        ))
                .subscribe();
    }

    @Override
    public Mono<Void> addLog(String logText, LOG_LEVEL logLevel, String traceId, String userId) {
        return Mono.fromRunnable(() -> {
            LogEvent log = new LogEvent(
                    Instant.now().toString(),
                    logLevel.toString(),
                    podId,
                    traceId,
                    userId,
                    logText
            );
            Sinks.EmitResult result = logEvents.tryEmitNext(log);
            if(result.isFailure()){
                health.setIsLogNetworkReady(false);
            }
        }).then();
    }

    @Override
    public Mono<String> addLogAndGetIt(String logText, LOG_LEVEL logLevel, String traceId, String userId) {
        return Mono.fromSupplier(() -> {
            LogEvent log = new LogEvent(
                    Instant.now().toString(),
                    logLevel.toString(),
                    podId,
                    traceId,
                    userId,
                    logText
            );
            Sinks.EmitResult result = logEvents.tryEmitNext(log);
            if(result.isFailure()){
                health.setIsLogNetworkReady(false);
            }
            return log.toString();
        });
    }
}
