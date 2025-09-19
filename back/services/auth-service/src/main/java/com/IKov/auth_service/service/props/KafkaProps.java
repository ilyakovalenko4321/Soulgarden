package com.IKov.auth_service.service.props;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaProps {

    private String publicKeyTopic;

    public KafkaProps() {
    }

    public KafkaProps(String publicKeyTopic) {
        this.publicKeyTopic = publicKeyTopic;
    }

    public String getPublicKeyTopic() {
        return publicKeyTopic;
    }

    public void setPublicKeyTopic(String publicKeyTopic) {
        this.publicKeyTopic = publicKeyTopic;
    }

}
