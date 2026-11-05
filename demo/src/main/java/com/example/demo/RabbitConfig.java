package com.example.demo;

import com.rabbitmq.client.DefaultSaslConfig;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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

    /** Serializza/deserializza i record in JSON (usato da RabbitTemplate e @RabbitListener). */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
