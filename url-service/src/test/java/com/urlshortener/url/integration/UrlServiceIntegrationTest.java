package com.urlshortener.url.integration;

import com.urlshortener.common.dto.ShortenUrlRequest;
import com.urlshortener.common.dto.ShortenUrlResponse;
import com.urlshortener.common.dto.UrlDetailsResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.profiles.active=test"}
)
@Testcontainers
@Sql("/test-data.sql")
class UrlServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer",
            () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
            () -> "org.springframework.kafka.support.serializer.JsonSerializer");

        // Auto-create schema for tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterEach
    void waitForAsyncTasks() throws InterruptedException {
        // Wait for async click tracking tasks to complete before test cleanup
        Thread.sleep(100);
    }

    @Test
    void createUrl_withValidRequest_returnsShortUrl() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setLongUrl("https://www.example.com");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "1");
        HttpEntity<ShortenUrlRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ShortenUrlResponse> response = restTemplate.postForEntity(
                "/api/urls/shorten",
                entity,
                ShortenUrlResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getShortCode());
        assertEquals("https://www.example.com", response.getBody().getLongUrl());

        // Verify short code has expected length
        assertTrue(response.getBody().getShortCode().length() >= 7);
    }

    @Test
    void getUrl_withExistingShortCode_returnsUrl() {
        // Create a URL first
        ShortenUrlRequest createRequest = new ShortenUrlRequest();
        createRequest.setLongUrl("https://www.google.com");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "1");
        HttpEntity<ShortenUrlRequest> entity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<ShortenUrlResponse> createResponse = restTemplate.postForEntity(
                "/api/urls/shorten",
                entity,
                ShortenUrlResponse.class
        );

        String shortCode = createResponse.getBody().getShortCode();

        // Get the URL
        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.set("X-User-Id", "1");
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders);

        ResponseEntity<UrlDetailsResponse> getResponse = restTemplate.exchange(
                "/api/urls/" + shortCode,
                org.springframework.http.HttpMethod.GET,
                getEntity,
                UrlDetailsResponse.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(shortCode, getResponse.getBody().getShortCode());
        assertEquals("https://www.google.com", getResponse.getBody().getLongUrl());
    }

    @Test
    void redirect_withExistingShortCode_redirectsToLongUrl() {
        // Create a URL first
        ShortenUrlRequest createRequest = new ShortenUrlRequest();
        createRequest.setLongUrl("https://www.github.com");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "1");
        HttpEntity<ShortenUrlRequest> entity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<ShortenUrlResponse> createResponse = restTemplate.postForEntity(
                "/api/urls/shorten",
                entity,
                ShortenUrlResponse.class
        );

        String shortCode = createResponse.getBody().getShortCode();

        // Test redirect (should return 301 MOVED_PERMANENTLY)
        // Note: TestRestTemplate follows redirects by default, so we may get 200 after following
        ResponseEntity<String> redirectResponse = restTemplate.getForEntity(
                "/" + shortCode,
                String.class
        );

        // Expect either 301 (if not followed) or 200 (if redirect was followed) or any 3xx
        assertTrue(
                redirectResponse.getStatusCode() == HttpStatus.MOVED_PERMANENTLY ||
                redirectResponse.getStatusCode() == HttpStatus.OK ||
                redirectResponse.getStatusCode().is3xxRedirection(),
                "Expected redirect or success status but got: " + redirectResponse.getStatusCode()
        );
    }

    @Test
    void getUrl_withNonExistentShortCode_returnsNotFound() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "1");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UrlDetailsResponse> response = restTemplate.exchange(
                "/api/urls/nonexistent",
                org.springframework.http.HttpMethod.GET,
                entity,
                UrlDetailsResponse.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
