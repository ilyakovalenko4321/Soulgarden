package com.IKov.log_service.config;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;

@Configuration
public class ElasticsearchConfig {

    @Bean(destroyMethod = "close")
    public RestClient restClient(
            @Value("${elasticsearch.host:elasticsearch}") String host,
            @Value("${elasticsearch.port:9200}") int port,
            @Value("${elasticsearch.scheme:http}") String scheme) {

        return RestClient.builder(new HttpHost(host, port, scheme)).build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient,
                                                   @Value("${health.es.retry.maxAttempts:3}") int maxAttempts,
                                                   @Value("${health.es.retry.initialIntervalMs:10000}") long initialIntervalMs,
                                                   @Value("${health.es.retry.maxIntervalMs:30000}") long maxIntervalMs,
                                                   @Value("${health.es.retry.multiplier:1.5}") double multiplier) {

        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(initialIntervalMs);
        backOff.setMaxInterval(maxIntervalMs);
        backOff.setMultiplier(multiplier);
        retryTemplate.setBackOffPolicy(backOff);


        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        return retryTemplate.execute(context -> {
            ElasticsearchClient client = new ElasticsearchClient(transport);

            try {
                // делаем быстрый ping — если false или бросается исключение, тригерим retry
                boolean ok = client.ping().value();
                if (!ok) {
                    System.out.println("Ошибка?");
                    throw new IllegalStateException("Elasticsearch ping returned false");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to ping Elasticsearch", e);
            }
            return client;
        });
    }

}
