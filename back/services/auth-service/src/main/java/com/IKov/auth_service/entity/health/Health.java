package com.IKov.auth_service.entity.health;

import org.springframework.stereotype.Component;

@Component
public class Health {

    private Boolean isContainerLive;
    private Boolean isKafkaReady;
    private Boolean isLogNetworkReady;

    public Health(){
        this.isKafkaReady = false;
        this.isContainerLive = true;
        this.isLogNetworkReady = true;
    }

    public void setIsKafkaReady(Boolean isReady){
        this.isKafkaReady = isReady;
    }

    public Boolean getIsKafkaReady(){
        return this.isKafkaReady;
    }

    public void setIsContainerLive(Boolean isLive){
        this.isContainerLive = isLive;
    }

    public Boolean getIsContainerLive(){
        return this.isContainerLive;
    }

    public void setIsLogNetworkReady(Boolean isReady){
        this.isLogNetworkReady = isReady;
    }

    public Boolean getIsLogNetworkIsReady(){
        return this.isLogNetworkReady;
    }
}

