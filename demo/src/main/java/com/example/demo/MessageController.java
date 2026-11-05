package com.example.demo;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    private final RabbitTemplate rabbitTemplate;
    private final String queue;

    public MessageController(RabbitTemplate rabbitTemplate, @Value("${app.queue}") String queue) {
        this.rabbitTemplate = rabbitTemplate;
        this.queue = queue;
    }

    @PostMapping("/messages")
    public String send(@RequestBody Notification message) {
        rabbitTemplate.convertAndSend(queue, message);
        return "Inviato: " + message;
    }
}
