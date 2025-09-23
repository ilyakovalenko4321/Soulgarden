package com.IKov.auth_service.service;

import com.IKov.auth_service.entity.logs.LogEvent;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LogShipper {

    Mono<Optional<List<LogEvent>>> sendBatch(List<LogEvent> logEvents);

}