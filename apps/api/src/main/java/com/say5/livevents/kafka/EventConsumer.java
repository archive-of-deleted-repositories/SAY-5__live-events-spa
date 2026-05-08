package com.say5.livevents.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.say5.livevents.domain.LiveEvent;
import com.say5.livevents.store.EventWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final EventWriter writer;
    private final EventBroadcaster broadcaster;
    private final EventDeserializer deserializer;

    public EventConsumer(EventWriter writer, EventBroadcaster broadcaster, ObjectMapper mapper) {
        this.writer = writer;
        this.broadcaster = broadcaster;
        this.deserializer = new EventDeserializer(mapper);
    }

    @KafkaListener(topics = "${livevents.kafka.topic:events.live}",
                   groupId = "${spring.kafka.consumer.group-id:livevents-api}")
    public void onMessage(String raw) {
        try {
            LiveEvent event = deserializer.parse(raw);
            writer.persist(event);
            broadcaster.publish(event);
        } catch (IllegalArgumentException ex) {
            log.warn("dropping unparseable event: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("failed to handle event", ex);
        }
    }
}
