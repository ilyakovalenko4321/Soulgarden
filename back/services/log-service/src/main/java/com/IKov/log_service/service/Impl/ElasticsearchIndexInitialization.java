package com.IKov.log_service.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.IKov.log_service.entity.health.Health;
import com.IKov.log_service.service.IndexInitialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnBean(ElasticsearchClient.class)
@Slf4j
public class ElasticsearchIndexInitialization implements IndexInitialization {

    private final ElasticsearchClient esClient;
    private final String indexName;
    private final String numberOfShards;
    private final String numberOfReplicas;
    private final Health health;

    public ElasticsearchIndexInitialization(ElasticsearchClient esClient,
                                            @Value("${elasticsearch.index.logs:logs}") String indexName,
                                            @Value("${elasticsearch.shards:1}") String numberOfShards,
                                            @Value("${elasticsearch.replicas:0}") String numberOfReplicas,
                                            Health health) {
        this.esClient = esClient;
        this.indexName = indexName;
        this.numberOfShards = numberOfShards;
        this.numberOfReplicas = numberOfReplicas;
        this.health = health;
    }


    @ConditionalOnBean()
    @Retryable(
            maxAttempts = 4,
            retryFor = {Exception.class},
            backoff = @Backoff(delay = 12_000, maxDelay = 40_000, multiplier = 1.5)
    )
    public void createIndex() {

        try {
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();

            if (!exists) {
                esClient.indices().create(c -> c
                        .index(indexName)
                        .mappings(m -> m
                                .properties("timestamp", Property.of(p -> p.date(d -> d)))
                                .properties("level", Property.of(p -> p.keyword(k -> k)))
                                .properties("podId", Property.of(p -> p.keyword(k -> k)))
                                .properties("traceId", Property.of(p -> p.keyword(k -> k)))
                                .properties("userId", Property.of(p -> p.keyword(k -> k)))
                                .properties("message", Property.of(p -> p.text(
                                        t -> t.fields("keyword", f -> f.keyword(k -> k))))))
                        .settings(s -> s.numberOfShards(numberOfShards).numberOfReplicas(numberOfReplicas))
                );
                log.info("Success");
            }

            health.setIsElasticsearchReachable(true);

        } catch (IOException e) {
            health.setIsElasticsearchReachable(false);
            throw new RuntimeException(e);
        }

    }

    @Recover
    public void recover() {
        health.setIsContainerLive(false);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady(ApplicationReadyEvent e) {
        e.getApplicationContext()
                .getBean(ElasticsearchIndexInitialization.class)
                .createIndex();
    }

}
