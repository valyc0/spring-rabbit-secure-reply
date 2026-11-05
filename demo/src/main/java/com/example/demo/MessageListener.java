package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    /**
     * Consuma il messaggio. Il valore restituito viene inviato da Spring AMQP alla coda
     * indicata nella proprieta' replyTo del messaggio; se e' null (nessun replyTo) non risponde.
     */
    @RabbitListener(queues = "${app.queue}")
    public Notification receive(Notification message,
                                @Header(name = AmqpHeaders.REPLY_TO, required = false) String replyTo) {
        log.info("Messaggio ricevuto: {} (replyTo={})", message, replyTo);
        if (replyTo == null) {
            return null;
        }
        return new Notification("listener", "Re: " + message.text(), System.currentTimeMillis());
    }

    /** Consumer della coda di risposta A (primo elemento di app.reply-queues). */
    @RabbitListener(queues = "#{'${app.reply-queues}'.split(',')[0]}")
    public void receiveReplyA(Notification reply) {
        log.info("Risposta A ricevuta: {}", reply);
    }

    /** Consumer della coda di risposta B (secondo elemento di app.reply-queues). */
    @RabbitListener(queues = "#{'${app.reply-queues}'.split(',')[1]}")
    public void receiveReplyB(Notification reply) {
        log.info("Risposta B ricevuta: {}", reply);
    }
}
