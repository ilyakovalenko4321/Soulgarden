package com.IKov.auth_service.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class PodIdConfig {

    @Value("${pod.id:}")
    private String idFromEnv;

    @Bean("podId")
    public String podId(){
        if(idFromEnv!=null && !idFromEnv.isEmpty()){
            return idFromEnv;
        }else{
            return UUID.randomUUID().toString();
        }
    }

}
