package com.codedecode.kafkademo.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ConsumerService {

  /*  @KafkaListener(topics = "codedecode_topic", groupId= "codedecode_group")
    public void listenToTopic(String message){
        System.out.println("the message is received: "+ message);
    }*/
}
