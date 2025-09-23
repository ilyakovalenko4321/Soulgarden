package com.IKov.log_service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/postLogBatch")
public class LogController {

    @PostMapping
    public ResponseEntity<Void> receiveLogs(@RequestBody List<LogEvent> logs) {
        if (logs != null) {
            logs.forEach(log -> System.out.println("Received log: " + log));
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/test")
    public String r(){
        return "ff";
    }

}
