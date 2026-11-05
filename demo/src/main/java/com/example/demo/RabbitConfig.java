package com.example.demo;

import com.rabbitmq.client.DefaultSaslConfig;
import org.springframework.boot.autoconfigure.amqp.ConnectionFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /** Autenticazione col certificato client (SASL EXTERNAL): nessun user/password. */
    @Bean
    ConnectionFactoryCustomizer externalAuth() {
        return cf -> cf.setSaslConfig(DefaultSaslConfig.EXTERNAL);
    }
}
