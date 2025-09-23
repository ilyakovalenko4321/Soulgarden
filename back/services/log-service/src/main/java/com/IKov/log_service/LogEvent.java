package com.IKov.log_service;

public record LogEvent(
        String timestamp,
        String level,
        String podId,
        String traceId,
        String userId,
        String message
) {

    @Override
    public String toString() {
        return String.format(
                "{\"timestamp\":\"%s\",\"level\":\"%s\",\"podId\":\"%s\",\"traceId\":\"%s\",\"userId\":\"%s\",\"message\":\"%s\"}",
                timestamp, level, podId, traceId, userId, message.replace("\"", "'")
        );
    }

}