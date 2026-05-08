package com.say5.livevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Full-stack integration test: real Kafka + real Postgres via testcontainers.
 *
 * Skipped unless the {@code RUN_TESTCONTAINERS} environment variable is set
 * (CI sets this; local developer runs that don't have a Docker daemon
 * reachable from the JVM are not blocked).
 */
@Testcontainers
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_TESTCONTAINERS", matches = ".*")
class KafkaSseIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Container
    @ServiceConnection
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void overrides(DynamicPropertyRegistry registry) {
        registry.add("livevents.kafka.topic", () -> "events.live");
    }

    @Autowired
    private WebApplicationContext context;

    @Test
    void producedEventsArePersistedAndReadableViaHistoryEndpoint() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (KafkaProducer<String, String> producer = makeProducer()) {
            for (int i = 0; i < 5; i++) {
                ObjectNode node = mapper.createObjectNode();
                node.put("id", UUID.randomUUID().toString());
                node.put("timestamp", "2026-04-0" + (i + 1) + "T00:00:00Z");
                node.put("type", "order");
                node.put("severity", "info");
                node.put("source", "kafka-it");
                node.set("payload", mapper.createObjectNode().put("i", i));
                producer.send(new ProducerRecord<>("events.live", node.toString())).get(5, TimeUnit.SECONDS);
            }
            producer.flush();
        }

        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            String body = mvc.perform(get("/api/events").param("limit", "10"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(body).contains("kafka-it");
            // expect all 5 entries to appear (count occurrences of source field)
            int count = body.split("kafka-it", -1).length - 1;
            assertThat(count).isGreaterThanOrEqualTo(5);
        });
    }

    private static KafkaProducer<String, String> makeProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaProducer<>(props);
    }
}
