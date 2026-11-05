package com.example.demo;

import java.util.List;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    Queue demoQueue(@Value("${app.queue}") String name) {
        return new Queue(name, true);
    }

    /** Code di risposta, dichiarate da yml (app.reply-queues). */
    @Bean
    Declarables replyQueues(@Value("${app.reply-queues}") List<String> names) {
        return new Declarables(names.stream().map(n -> new Queue(n, true)).toList());
    }
}
