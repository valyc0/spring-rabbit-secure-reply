package com.example.demo;

import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MessageController {

    private final RabbitTemplate rabbitTemplate;
    private final String queue;
    private final List<String> replyQueues;

    public MessageController(RabbitTemplate rabbitTemplate,
                             @Value("${app.queue}") String queue,
                             @Value("${app.reply-queues}") List<String> replyQueues) {
        this.rabbitTemplate = rabbitTemplate;
        this.queue = queue;
        this.replyQueues = replyQueues;
    }

    /**
     * Invia il messaggio in coda. Se replyTo e' indicato, il listener rispondera' su quella coda
     * (deve essere una delle code configurate in app.reply-queues).
     */
    @PostMapping("/messages")
    public String send(@RequestBody Notification message,
                       @RequestParam(required = false) String replyTo) {
        if (replyTo != null && !replyQueues.contains(replyTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "replyTo non valido, ammessi: " + replyQueues);
        }
        rabbitTemplate.convertAndSend(queue, message, m -> {
            m.getMessageProperties().setReplyTo(replyTo);
            return m;
        });
        return "Inviato: " + message + (replyTo != null ? " (risposta su " + replyTo + ")" : "");
    }
}
