package com.say5.livevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.say5.livevents.domain.EventType;
import com.say5.livevents.domain.LiveEvent;
import com.say5.livevents.domain.Severity;
import com.say5.livevents.kafka.EventDeserializer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventDeserializerTest {

    private final EventDeserializer deserializer = new EventDeserializer(new ObjectMapper());

    @Test
    void parsesFullyPopulatedRecord() {
        String json = """
                {
                  "id":"00000000-0000-0000-0000-000000000001",
                  "timestamp":"2026-05-01T12:00:00Z",
                  "type":"order",
                  "severity":"warning",
                  "source":"checkout",
                  "payload":{"amount":42}
                }
                """;
        LiveEvent ev = deserializer.parse(json);
        assertThat(ev.type()).isEqualTo(EventType.ORDER);
        assertThat(ev.severity()).isEqualTo(Severity.WARNING);
        assertThat(ev.timestamp()).isEqualTo(Instant.parse("2026-05-01T12:00:00Z"));
        assertThat(ev.source()).isEqualTo("checkout");
        assertThat(ev.payload()).contains("\"amount\":42");
    }

    @Test
    void defaultsAppliedWhenOptionalFieldsAreMissing() {
        String json = """
                {"type":"alert"}
                """;
        LiveEvent ev = deserializer.parse(json);
        assertThat(ev.type()).isEqualTo(EventType.ALERT);
        assertThat(ev.severity()).isEqualTo(Severity.INFO);
        assertThat(ev.source()).isEqualTo("unknown");
        assertThat(ev.payload()).isEqualTo("{}");
        assertThat(ev.id()).isNotNull();
        assertThat(ev.timestamp()).isNotNull();
    }

    @Test
    void missingTypeIsRejected() {
        assertThatThrownBy(() -> deserializer.parse("{\"severity\":\"info\"}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidJsonIsRejected() {
        assertThatThrownBy(() -> deserializer.parse("not-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
