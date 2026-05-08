package com.say5.livevents.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.say5.livevents.domain.EventType;
import com.say5.livevents.domain.LiveEvent;
import com.say5.livevents.domain.Severity;

import java.time.Instant;
import java.util.UUID;

/**
 * Translates a raw JSON Kafka record into a {@link LiveEvent}. Strict but
 * forgiving: missing optional fields fall back to sensible defaults, missing
 * required fields throw.
 */
public final class EventDeserializer {

    private final ObjectMapper mapper;

    public EventDeserializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public LiveEvent parse(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            UUID id = node.hasNonNull("id")
                    ? UUID.fromString(node.get("id").asText())
                    : UUID.randomUUID();
            Instant ts = node.hasNonNull("timestamp")
                    ? Instant.parse(node.get("timestamp").asText())
                    : Instant.now();
            EventType type = EventType.parse(textOrNull(node, "type"))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "missing or invalid 'type' field"));
            Severity severity = Severity.parse(textOrNull(node, "severity"))
                    .orElse(Severity.INFO);
            String source = node.hasNonNull("source") ? node.get("source").asText() : "unknown";
            JsonNode payloadNode = node.get("payload");
            String payload = payloadNode == null || payloadNode.isNull()
                    ? "{}"
                    : payloadNode.toString();
            return new LiveEvent(id, ts, type, severity, source, payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("could not parse event payload: " + e.getMessage(), e);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
