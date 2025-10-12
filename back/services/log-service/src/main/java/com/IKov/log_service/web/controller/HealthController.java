package com.IKov.log_service.web.controller;

import com.IKov.log_service.entity.health.Health;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("health/actuator")
public class HealthController {

    private Health health;

    public HealthController(Health health){
        this.health = health;
    }

    @GetMapping("/liveness")
    public ResponseEntity<Void> livenessProbe(){
        if(health.isContainerLive()){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping("/readiness")
    public ResponseEntity<Void> readinessProbe(){
        if(health.isContainerReady()){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

}
