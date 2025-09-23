package com.IKov.auth_service.service.Impl;

import com.IKov.auth_service.entity.health.Health;
import com.IKov.auth_service.entity.logs.LogEvent;
import com.IKov.auth_service.service.LogShipper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
public class HttpLogShipper implements LogShipper {

    private final WebClient webClient;

    public HttpLogShipper(WebClient.Builder webClientBuilder,
                          @Value("${logs.base-url:http://localhost:8082}") String baseUrl,
                          Health health){
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<Optional<List<LogEvent>>> sendBatch(List<LogEvent> logEvents) {
        System.out.println("sendBatchWork");
        return webClient.post()
                .uri("/postLogBatch")
                .bodyValue(logEvents)
                .retrieve()
                .toBodilessEntity()
                .map(resp -> Optional.<List<LogEvent>>empty()) // успех → пусто
                .onErrorReturn(Optional.of(logEvents));
    }
}
