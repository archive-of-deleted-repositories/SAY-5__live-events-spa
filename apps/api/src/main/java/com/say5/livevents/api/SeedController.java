package com.say5.livevents.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.say5.livevents.domain.LiveEvent;
import com.say5.livevents.kafka.EventBroadcaster;
import com.say5.livevents.kafka.EventDeserializer;
import com.say5.livevents.store.EventWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only event seeder. Bypasses Kafka and writes directly to the
 * read-model + broadcaster so e2e tests don't need a live producer.
 *
 * Disabled by default. Enable with {@code livevents.seed.enabled=true} in
 * docker-compose / e2e environments only.
 */
@RestController
@RequestMapping("/api/events")
@ConditionalOnProperty(name = "livevents.seed.enabled", havingValue = "true")
public class SeedController {

    private final EventWriter writer;
    private final EventBroadcaster broadcaster;
    private final EventDeserializer deserializer;

    public SeedController(EventWriter writer, EventBroadcaster broadcaster, ObjectMapper mapper) {
        this.writer = writer;
        this.broadcaster = broadcaster;
        this.deserializer = new EventDeserializer(mapper);
    }

    @PostMapping("/_seed")
    public EventDto seed(@RequestBody JsonNode body) {
        LiveEvent ev = deserializer.parse(body.toString());
        writer.persist(ev);
        broadcaster.publish(ev);
        return EventDto.from(ev);
    }
}
