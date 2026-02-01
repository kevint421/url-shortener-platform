package com.urlshortener.analytics.integration;

import com.urlshortener.analytics.entity.UrlClick;
import com.urlshortener.analytics.repository.UrlClickRepository;
import com.urlshortener.common.event.UrlAccessedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class AnalyticsEventConsumerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // Configure Kafka producer for tests to use JSON serialization
        registry.add("spring.kafka.producer.value-serializer",
            () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.producer.key-serializer",
            () -> "org.apache.kafka.common.serialization.StringSerializer");

        // Auto-create schema for tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private KafkaTemplate<String, UrlAccessedEvent> kafkaTemplate;

    @Autowired
    private UrlClickRepository urlClickRepository;

    @Test
    void consumeUrlAccessEvent_savesClickToDatabase() {
        // Create and send event
        UrlAccessedEvent event = new UrlAccessedEvent();
        event.setShortCode("test123");
        event.setAccessedAt(LocalDateTime.now());
        event.setIpAddress("192.168.1.1");
        event.setUserAgent("Mozilla/5.0");
        event.setReferer("https://google.com");

        kafkaTemplate.send("url-access-events", event);

        // Wait for event to be consumed and processed
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    UrlClick click = urlClickRepository.findAll()
                            .stream()
                            .filter(c -> c.getShortCode().equals("test123"))
                            .findFirst()
                            .orElse(null);

                    assertNotNull(click, "Click should be saved to database");
                    assertEquals("test123", click.getShortCode());
                    assertEquals("192.168.1.1", click.getIpAddress());
                    assertEquals("Mozilla/5.0", click.getUserAgent());
                    assertEquals("https://google.com", click.getReferer());
                });
    }

    @Test
    void consumeMultipleEvents_savesAllToDatabase() {
        // Send multiple events
        for (int i = 0; i < 3; i++) {
            UrlAccessedEvent event = new UrlAccessedEvent();
            event.setShortCode("multi" + i);
            event.setAccessedAt(LocalDateTime.now());
            event.setIpAddress("10.0.0." + i);
            event.setUserAgent("TestAgent");

            kafkaTemplate.send("url-access-events", event);
        }

        // Wait for all events to be processed
        await()
                .atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long count = urlClickRepository.findAll()
                            .stream()
                            .filter(c -> c.getShortCode().startsWith("multi"))
                            .count();

                    assertEquals(3, count, "All 3 clicks should be saved");
                });
    }
}
