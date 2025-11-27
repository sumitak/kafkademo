package com.codedecode.kafkademo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProducerService {

    @Autowired
    KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

   /*  public void sendMsgToTopic(String message){
         kafkaTemplate.send("codedecode_topic", message);
     }*/
}
