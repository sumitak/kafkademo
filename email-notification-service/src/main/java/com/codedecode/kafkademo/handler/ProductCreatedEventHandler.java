package com.codedecode.kafkademo.handler;

import com.codedecode.kafkademo.entity.ProcessedEventEntity;
import com.codedecode.kafkademo.error.NotRetryableException;
import com.codedecode.kafkademo.error.RetryableException;
import com.codedecode.kafkademo.repo.ProcessedEventRepository;
import com.codedecode.kafkademo.service.ProductCreatedEvent;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;

@Component
//@KafkaListener(topics="product-created-events-topic") //it can be assigned here to avoid specify under each method name
public class ProductCreatedEventHandler {
    private final Logger logger = LoggerFactory.getLogger(ProductCreatedEventHandler.class);
    private RestTemplate restTemplate;
    private ProcessedEventRepository processedEventRepository;

    public ProductCreatedEventHandler(RestTemplate restTemplate, ProcessedEventRepository processedEventRepository) {
        this.restTemplate = restTemplate;
        this.processedEventRepository = processedEventRepository;
    }


    // this annotation can be used above the class name or method name
    //It is used to mark method or class as a target for incoming messages from Kafka topic
    //that means this method of class should be invoked whenever a new message is received from
    //specified Kafka topic
    @Transactional
    @KafkaListener(topics="product-created-events-topic")
    @KafkaHandler //specifies the method handles event
    public void handle(@Payload ProductCreatedEvent event,
                       @Header("messageId") String messageId,
                       @Header(KafkaHeaders.RECEIVED_KEY) String messageKey){
     //   if(true) throw new NotRetryableException("Not Retryable");
        logger.info("**** Received product created event ****");
        logger.info("title id: " + event.getTitle());

        //check if the message was already processed before
       ProcessedEventEntity existingProcessedEventEntity = processedEventRepository.findByMessageId(messageId);
       if(existingProcessedEventEntity != null){

           logger.info("**** Found existing product created event ****");
           return ;
       }
        String requestUrl = "http://localhost:5053/response/200";
        try {
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, null, String.class);

            if (response.getStatusCode().value() == HttpStatus.OK.value()) {
                logger.info("received response from remote microservice");
            }
        }catch (ResourceAccessException e){
            logger.error("Unable to access remote microservice");
            throw new RetryableException(e);
        }catch (HttpServerErrorException e ){
            logger.error(e.getMessage());
            throw new NotRetryableException(e);
        }catch(Exception e){
            logger.error(e.getMessage());
            throw new NotRetryableException(e);
        }
        try {
            processedEventRepository.save(new ProcessedEventEntity(messageId, event.getProductId()));
        }catch (DataIntegrityViolationException e){
            logger.error(e.getMessage());
            throw new NotRetryableException(e);

        }
    }
}
