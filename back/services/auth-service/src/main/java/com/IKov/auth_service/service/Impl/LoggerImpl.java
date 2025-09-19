package com.IKov.auth_service.service.Impl;

import com.IKov.auth_service.service.Logger;
import org.springframework.stereotype.Service;

@Service
public class LoggerImpl implements Logger
{

    private final String podId;

    public LoggerImpl(String podId){
        this.podId = podId;
    }

    @Override
    public String formLog(String logText) {
        return "[" + podId + "]" + logText;
    }
}
