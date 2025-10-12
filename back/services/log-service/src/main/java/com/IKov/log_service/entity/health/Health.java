package com.IKov.log_service.entity.health;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class Health {

    private Boolean isElasticsearchReachable;

    private Boolean isContainerLive;

    public Health(){
        this.isContainerLive = true;
        this.isElasticsearchReachable = false;
    }

    public Boolean isContainerLive(){
        return getIsContainerLive();
    }

    public Boolean isContainerReady(){
        return getIsElasticsearchReachable();
    }
}
