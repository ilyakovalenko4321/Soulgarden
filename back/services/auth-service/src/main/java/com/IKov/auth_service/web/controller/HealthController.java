package com.IKov.auth_service.web.controller;

import com.IKov.auth_service.entity.health.Health;
import com.IKov.auth_service.entity.logs.LOG_LEVEL;
import com.IKov.auth_service.service.Logger;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/actuator/health")
@Slf4j
public class HealthController {

    private final Health health;
    private final Logger logger;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HealthController.class);

    public HealthController(Health health, Logger logger){
        this.health = health;
        this.logger = logger;
    }

    @GetMapping("/liveness")
    public ResponseEntity<String> livenessProbe(){
        boolean containerLive = health.getIsContainerLive();
        log.info("[LIVENESS] Probe called. Container live = {}", containerLive);
        if(health.getIsContainerLive()){
            return ResponseEntity.ok("OK");
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(logger.addLogAndGetIt
                ("Unable to send public key", LOG_LEVEL.WARN, MDC.get("traceId"), MDC.get("userId")).block());
    }

    @GetMapping("/readiness")
    public ResponseEntity<String> readinessProbe(){
        boolean kafkaReady = health.getIsKafkaReady();
        log.info("[READINESS] Probe called. Kafka ready = {}", kafkaReady);
        if(health.getIsKafkaReady() && health.getIsLogNetworkIsReady()){
            return ResponseEntity.ok("OK");
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(logger
                .addLogAndGetIt("Kafka do not receive message", LOG_LEVEL.WARN, MDC.get("traceId"), MDC.get("userId")).block());
    }

}
