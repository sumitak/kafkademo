package com.codedecode.kafkademo.handler;

import com.codedecode.kafkademo.service.ProductCreatedEvent;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
//@KafkaListener(topics="product-created-events-topic") //it can be assigned here to avoid specify under each method name
public class ProductCreatedEventHandler {
    private final Logger logger = Logger.getLogger(ProductCreatedEventHandler.class.getName());

    // this annotation can be used above the class name or method name
    //It is used to mark method or class as a target for incoming messages from Kafka topic
    //that means this method of class should be invoked whenever a new message is received from
    //specified Kafka topic
    @KafkaListener(topics="product-created-events-topic")
    @KafkaHandler //specifies the method handles event
    public void handle(ProductCreatedEvent event){
        logger.info("**** Received product created event ****");
        logger.info("title id: " + event.getTitle());

    }
}
