package com.codedecode.kafkademo.service;

import com.codedecode.kafkademo.model.CreateProductRestModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.core.KafkaTemplate;
import  org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ProductServiceImpl implements ProductService{

    private final Logger LOGGER= LoggerFactory.getLogger("ProductServiceImpl.class");

    KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    public ProductServiceImpl(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public String createProduct(CreateProductRestModel productRestModel) throws Exception{

        String productId = UUID.randomUUID().toString();
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent(productId,
                productRestModel.getTitle(),
                productRestModel.getPrice(),
                productRestModel.getQuantity());
        /*
        * Line#34 to Line$45 is asynchronous call,
        * if you add future.join()(at line#47), the thread wait for response from Kafka broker
        * the below code may confuse the developer, so there is another way to get the response from boker
        * */
/*        CompletableFuture<SendResult<String, ProductCreatedEvent>> future =
                        kafkaTemplate.send("product-created-events-topic",
                                productId,productCreatedEvent);
        future.whenComplete((result, exception) -> {
            if(exception != null){
                LOGGER.error("Failed to send message: "+exception.getMessage());
            }else{
                LOGGER.info("Message sent successfully: "+result.getRecordMetadata());
            }
        });
        future.join(); // synchronous message call. */
        LOGGER.info("**** Before publishing product create event ****");
        SendResult<String, ProductCreatedEvent> result = kafkaTemplate.send("product-created-events-topic",
                productId,productCreatedEvent).get(); // send() method may be slow here to process since it waits for response from all
        // Kafka brokers that my message is stored in Kafka Topic successfully.
        LOGGER.info("Topic: {}", result.getRecordMetadata().topic());
        LOGGER.info("Partition: {}", result.getRecordMetadata().partition());
        LOGGER.info("Offset: {}", result.getRecordMetadata().offset());
        LOGGER.info("**** Returning productId **********");
        return productId;
    }
}
