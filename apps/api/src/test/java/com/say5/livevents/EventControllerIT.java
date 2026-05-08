package com.say5.livevents;

import com.say5.livevents.domain.EventType;
import com.say5.livevents.domain.LiveEvent;
import com.say5.livevents.domain.Severity;
import com.say5.livevents.store.EventRepository;
import com.say5.livevents.store.EventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@ActiveProfiles("test")
class EventControllerIT {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EventWriter writer;

    @Autowired
    private EventRepository repository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        repository.deleteAll();
    }

    @Test
    void historyReturnsRecentEventsNewestFirst() throws Exception {
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < 5; i++) {
            writer.persist(new LiveEvent(
                    UUID.randomUUID(),
                    base.plusSeconds(i),
                    EventType.ORDER,
                    Severity.INFO,
                    "test",
                    "{\"i\":" + i + "}"));
        }

        var response = mvc.perform(get("/api/events").param("limit", "5"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        String body = response.getContentAsString();
        // newest first: i=4 should appear before i=0
        assertThat(body).contains("\"i\":4");
        assertThat(body).contains("\"i\":0");
        int idx4 = body.indexOf("\"i\":4");
        int idx0 = body.indexOf("\"i\":0");
        assertThat(idx4).isLessThan(idx0);
    }

    @Test
    void filtersByTypeAndSeverity() throws Exception {
        Instant base = Instant.parse("2026-02-01T00:00:00Z");
        writer.persist(new LiveEvent(UUID.randomUUID(), base, EventType.ORDER, Severity.INFO, "s", "{}"));
        writer.persist(new LiveEvent(UUID.randomUUID(), base.plusSeconds(1), EventType.ALERT, Severity.CRITICAL, "s", "{}"));

        var response = mvc.perform(get("/api/events")
                        .param("type", "alert")
                        .param("severity", "critical"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        String body = response.getContentAsString();
        assertThat(body).contains("\"type\":\"alert\"");
        assertThat(body).doesNotContain("\"type\":\"order\"");
    }

    @Test
    void csvExportStreamsHeaderAndRows() throws Exception {
        Instant base = Instant.parse("2026-03-01T00:00:00Z");
        writer.persist(new LiveEvent(UUID.randomUUID(), base, EventType.PAYMENT, Severity.INFO, "checkout", "{\"amount\":1}"));
        writer.persist(new LiveEvent(UUID.randomUUID(), base.plusSeconds(1), EventType.PAYMENT, Severity.WARNING, "checkout", "{\"amount\":2}"));

        var response = mvc.perform(get("/api/events/export.csv").param("type", "payment"))
                .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).startsWith("text/csv");
        String body = response.getContentAsString();
        assertThat(body).startsWith("id,timestamp,type,severity,source,payload");
        assertThat(body).contains("payment,info,checkout");
        assertThat(body).contains("payment,warning,checkout");
    }

    @Test
    void invalidCursorReturnsErrorEnvelope() throws Exception {
        var response = mvc.perform(get("/api/events").param("cursor", "garbage"))
                .andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("\"code\":\"bad_request\"");
    }
}
