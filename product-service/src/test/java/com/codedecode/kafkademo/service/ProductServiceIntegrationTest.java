package com.codedecode.kafkademo.service;

import com.codedecode.kafkademo.model.CreateProductRestModel;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/***
 * Partition =3 --> each topic in embedded kafka topic has 3 partions
 * count=3 -> means no. of Kafka broker to start
 * controlledShutfown=true -> means, make sure that when Kafka server is shutting down, it will
 * try to migrate all leaders on the broker to another broker before it shuts down.In another words, it
 * enables gradual shutdown with smooth transition to minimize disruptions
 */

/**
 *  When we specify ActiveProfile as test, the application looks for property file ends with '-test'
 */

/**
 * TestInstance  -> This annotation is used to configure the lifecycle of test instances in JUnit 5.
 * By default, JUnit creates a new instance of the test class for each test method (PER_METHOD lifecycle).
 * However, by using @TestInstance(TestInstance.Lifecycle.PER_CLASS), we can change this behavior so that a single instance
 * of the test class is created and shared across all test methods within that class. This can be useful when we
 * want to maintain state or share resources between test methods, such as
 * database connections or mock objects.
 */


/**
 * DirtiesContext -> it will reset the spring context after each test method execution.
 * This is particularly useful in integration tests where the state of the application context
 * may be modified during the test execution, and we want to ensure that each test starts
 * with a clean and consistent state.
 */


@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(partitions=3,count=3, controlledShutdown = true)
@SpringBootTest(properties = "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    Environment environment;

    /**
     * KafkaMessageListenerContainer -> It is a Spring Kafka component that provides a way to consume messages
     * from Kafka topics in a more controlled manner. It is built on top of the Kafka consumer API and
     * provides additional features and abstractions to simplify message consumption in Spring applications.
     *
     * It serves as a bridge between the Kafka consumer and the Spring application, allowing developers to
     * easily integrate Kafka message consumption into their Spring-based applications.
     */

    private KafkaMessageListenerContainer<String, ProductCreatedEvent> container;

    /**
     * BlockingQueue -> It is a thread-safe data structure that allows multiple threads to
     * add and remove elements concurrently. It is part of the java.util.concurrent package and
     * provides a way to implement producer-consumer scenarios where one or more threads produce
     * data and one or more threads consume that data.
     */
    private BlockingQueue<ConsumerRecord<String, ProductCreatedEvent>> records;

    /**
     * ConsumerRecord -> It is a class provided by the Apache Kafka client library that represents
     * a single record (message) consumed from a Kafka topic. It contains information about the
     * topic, partition, offset, key, and value of the consumed message.
     *
     * Once the KafkaMessage ListenerContainer is started, it will begin consuming messages from the specified
     * Kafka topic(s) and passing them to the configured MessageListener for processing(whenever a message is received from Kafka topic).
     *
     * And once the message is received from Kafka topic, it is added to the records queue for further processing or verification in the test cases.
     */

    @BeforeAll
    void setup(){
        DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(getConsumerConfigs());
        ContainerProperties containerProperties = new ContainerProperties(environment.getProperty("product-created-event.topic.name"));
        container = new KafkaMessageListenerContainer<>(consumerFactory,containerProperties);
        /*
        * Here records is a BlockingQueue, so that when a message is received from Kafka topic,
        * it is added to the records queue for further processing or verification in the test cases.
         */
        records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, ProductCreatedEvent>) records :: add);
        container.start();
        /*
        * wait until the container has the required number of assigned partitions
        * before starting the test. This ensures that the container is fully initialized
        * and ready to consume messages from the Kafka topic.
         */
        ContainerTestUtils.waitForAssignment(container,embeddedKafkaBroker.getPartitionsPerTopic());
    }

    // name of the method name followed the format test<methodName(SystemUnderTest)>_when<condition or state change>_<expectedResult>
    @Test
    public void testCreateProduct_whenGivenValidProductDetails_successfulSendKafkaMessage() throws Exception {
        // 3 way pattern of writing test cases: Arrange, Act , Assert
        //Arrange
        String title = "Test Product";
        BigDecimal price = new BigDecimal("12.34");
        Integer quantity = 1;

        CreateProductRestModel createProductRestModel = new CreateProductRestModel();
        createProductRestModel.setTitle(title);
        createProductRestModel.setPrice(price);
        createProductRestModel.setQuantity(quantity);

        //Act
        productService.createProduct(createProductRestModel);

        //Assert
        /**
         * poll() -> retrieves and removes the head of this queue, waiting up to the specified wait time
         * if necessary for an element to become available.
         * If the specified wait time elapses before an element is available, this method returns null.
         *
         * Read consumer record from the BlockingQueue 'records' with a timeout of 3000 milliseconds (3 seconds).
         * If a record is available within this time, it is assigned to the 'message' variable for further processing or verification in the test case.
         */
        ConsumerRecord<String, ProductCreatedEvent> message = records.poll(3000, TimeUnit.MILLISECONDS);
        /**
         *  If message is null, thee assertion will fail, indicating that no message was received from the Kafka topic within the specified timeout period.
         */
        assertNotNull(message);
        assertNotNull(message.key());
        assertNotNull(message.value());

        ProductCreatedEvent productCreatedEvent =  message.value();
        assertNotNull(productCreatedEvent.getProductId());
        assert(productCreatedEvent.getTitle().equals(title));
        assert(productCreatedEvent.getPrice().compareTo(price) == 0);
        assert(productCreatedEvent.getQuantity().equals(quantity));

    }

    /**
     * Here instructor used embeddedKafkaBroker.getBrokersAsString() to get brokers address dynamically
     * There is a chance of change of port number at runtime, so it is better to get the broker address dynamically
     * So you cannot hardcode the port number like "localhost:9092" in your test configuration
     * @return
     */

    private Map<String, Object> getConsumerConfigs() {
        return   Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,embeddedKafkaBroker.getBrokersAsString(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class,
                ConsumerConfig.GROUP_ID_CONFIG, environment.getProperty("spring.kafka.consumer.group-id"),
                JsonDeserializer.TRUSTED_PACKAGES,  environment.getProperty("spring.kafka.consumer.properties.spring.json.trusted.packages"),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, environment.getProperty("spring.kafka.consumer.auto-offset-reset"));

    }

    /**
     * Tear down the Kafka container after all tests have run
     */
    @AfterAll
    void tearDown(){
        if(container != null){
            container.stop();
        }
    }
}
