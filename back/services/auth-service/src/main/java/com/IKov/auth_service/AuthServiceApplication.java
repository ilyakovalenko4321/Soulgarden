package com.IKov.auth_service;

import com.IKov.auth_service.service.props.KafkaProps;
import com.IKov.auth_service.service.props.ValidityProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableConfigurationProperties({ValidityProps.class, KafkaProps.class})
@EnableRetry
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
