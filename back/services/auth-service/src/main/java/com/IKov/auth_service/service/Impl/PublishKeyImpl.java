package com.IKov.auth_service.service.Impl;

import com.IKov.auth_service.entity.health.Health;
import com.IKov.auth_service.service.PublishKey;
import com.IKov.auth_service.service.props.KafkaProps;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;

@Service
@Slf4j
public class PublishKeyImpl implements PublishKey {

    private static final Logger log = LoggerFactory.getLogger(PublishKeyImpl.class);
    private final RSAPublicKey rsaPublicKey;
    private final String rsaKeyId;
    private final KafkaSender<String, String> kafkaSender;
    private final KafkaProps kafkaProps;
    private final com.IKov.auth_service.service.Logger logger;
    private final Health health;

    public PublishKeyImpl(RSAPublicKey rsaPublicKey, String rsaKeyId, KafkaSender<String, String> kafkaSender, KafkaProps kafkaProps,
                          com.IKov.auth_service.service.Logger logger, Health health){
        this.rsaPublicKey = rsaPublicKey;
        this.rsaKeyId = rsaKeyId;
        this.kafkaSender = kafkaSender;
        this.kafkaProps = kafkaProps;
        this.logger = logger;
        this.health = health;
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 10_000, multiplier = 1.5, maxDelay = 20_000)
    )
    public void publish() throws IOException {
        String topic = kafkaProps.getPublicKeyTopic();
        String key = rsaKeyId;
        String value = Base64.getEncoder().encodeToString(rsaPublicKey.getEncoded());

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        SenderRecord<String, String, Void> senderRecord = SenderRecord.create(record, null);

        try {
            kafkaSender.send(Mono.just(senderRecord))
                    .timeout(Duration.ofSeconds(30))
                    .doOnNext(r -> log.info(logger.formLog("Public key was successfully send")))
                    .retryWhen(
                            Retry.fixedDelay(3, Duration.ofSeconds(10))
                                    .doAfterRetry(s -> System.out.println("Send failed"))
                                    .transientErrors(true)
                    )
                    .blockLast();
        } catch (Exception e){
            log.error(logger.formLog("Cannot publish public key"));
            throw e;
        }
        health.setIsKafkaReady(true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishOnStartup() throws IOException {
        publish();
    }

    @Recover
    private void setHealthFalse(){
        log.info(logger.formLog("Error while sending kafka message. It will restart container"));
        health.setIsContainerLive(false);
    }

}
