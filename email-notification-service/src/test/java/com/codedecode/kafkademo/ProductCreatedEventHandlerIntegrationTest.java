package com.codedecode.kafkademo;

import com.codedecode.kafkademo.entity.ProcessedEventEntity;
import com.codedecode.kafkademo.handler.ProductCreatedEventHandler;
import com.codedecode.kafkademo.repo.ProcessedEventRepository;
import com.codedecode.kafkademo.service.ProductCreatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SpringBootTest annotation is used to create an application context for integration tests.
 * The properties attribute is used to override specific properties for the test context.
 * In this case, it sets the Kafka consumer's bootstrap servers to the embedded Kafka brokers
 * provided by the Spring Kafka test framework. This allows the test to interact with
 * the embedded Kafka instance instead of an external Kafka cluster.
 *It simulates the production environment as closely as possible, enabling comprehensive testing of
 * the application's Kafka-related functionality.
 *This means that all components, configurations, and beans defined in the main application context
 * will be loaded and available for testing within this test class.
 */

/**
 * By setting the property "spring.kafka.consumer.bootstrap-servers" to "${spring.embedded.kafka.brokers}",
 * we are configuring the Kafka consumer to connect to the embedded Kafka brokers that are started
 * automatically by the Spring Kafka test framework during the test execution. This ensures that the tests run against the embedded Kafka instance,
 * allowing for isolated and controlled testing of Kafka-related functionality without relying on an external Kafka cluster.
 */

/**
 * ${spring.embedded.kafka.brokers}" is a placeholder that gets resolved to the actual address of the embedded Kafka brokers
 * started by the Spring Kafka test framework. This allows the test to connect to the embedded Kafka instance
 * without hardcoding the broker addresses, making the tests more flexible and portable.
 */

/**
 * EmbeddedKafka annotation is used to set up an embedded Kafka broker for testing purposes.
 * This allows us to run integration tests that involve Kafka without needing a separate Kafka server.
 * The embedded Kafka broker is started automatically when the test context is initialized
 * and shut down when the tests are completed.
 */

/*@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)*/
@EmbeddedKafka
@SpringBootTest(properties="spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class ProductCreatedEventHandlerIntegrationTest {
/**
 * MokitoBean -> It is used to create a mock of a Spring bean in the application context.
 * This allows us to define custom behavior for the mocked bean during testing,
 * enabling us to isolate and test specific components without relying on the actual implementations.
 * */
    @MockitoBean
    ProcessedEventRepository processedEventRepository;

    @MockitoBean
    RestTemplate restTemplate;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate; // String is for message key, Object is for message value/payload

    /**
     * SpyBean -> It is used to create a spy of a Spring bean in the application context.
     * A spy allows us to monitor and verify interactions with the bean while still
     * allowing the original methods to be called unless explicitly stubbed.
     */
    @MockitoSpyBean
    ProductCreatedEventHandler eventHandler;

    @Test
    public void testProductCreatedEventHandler_OnProductCreated_HandlesEvent() throws Exception{
        //Arrange
        ProductCreatedEvent productCreatedEvent = new ProductCreatedEvent();
        productCreatedEvent.setPrice(new BigDecimal(10));
        productCreatedEvent.setQuantity(11);
        productCreatedEvent.setProductId(UUID.randomUUID().toString());
        productCreatedEvent.setTitle("test title");

        String messageId = UUID.randomUUID().toString();
        String messageKey = productCreatedEvent.getProductId();


        ProducerRecord<String, Object> record =
                new ProducerRecord<>("product-created-events-topic", messageKey, productCreatedEvent);
        record.headers().add("messageId", messageId.getBytes());
        record.headers().add("messageKey", messageKey.getBytes());

        ProcessedEventEntity existingProcessedEventEntity = new ProcessedEventEntity();
        existingProcessedEventEntity.setMessageId(messageId);
        existingProcessedEventEntity.setProductId(productCreatedEvent.getProductId());
        when(processedEventRepository.findByMessageId(anyString())).thenReturn(existingProcessedEventEntity);
     //   when(processedEventRepository.save(any(ProcessedEventEntity.class))).thenReturn(null);
        when(processedEventRepository.save(any())).thenReturn(null);


        String response = "{\"key\":\"value\"}";
        HttpHeaders headers= new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> responseEntity = new ResponseEntity<>(response, headers, HttpStatus.OK);
        when(restTemplate.exchange(any(String.class),
                any(HttpMethod.class),
                isNull(),
                eq(String.class)))
                .thenReturn(responseEntity);

        //Act
        kafkaTemplate.send(record).get();

        //Assert
        /**
         * ArgumentCaptor -> It is a class provided by the Mockito testing framework
         * that allows us to capture and inspect the arguments passed to a method during a test.
         * It is useful when we want to verify the values of the arguments passed to a method
         * or when we want to perform additional assertions on those arguments.
         */

        ArgumentCaptor<String> messageIdCaptor = ArgumentCaptor.forClass(String.class); // message id of type String data tyoe
        ArgumentCaptor<String> messageKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ProductCreatedEvent> processedEventEntityCaptor =
                ArgumentCaptor.forClass(ProductCreatedEvent.class);
        //verify that the handle method of eventHandler is called once with the expected parameters
       verify(eventHandler, timeout(5000).times(1))
               .handle(processedEventEntityCaptor.capture(),
                       messageIdCaptor.capture(),
                       messageKeyCaptor.capture()
                      );
       assertEquals(messageId, messageIdCaptor.getValue());
       assertEquals(messageKey, messageKeyCaptor.getValue());
       assertEquals(productCreatedEvent.getProductId(),
               processedEventEntityCaptor.getValue().getProductId()
               );

    }
}
