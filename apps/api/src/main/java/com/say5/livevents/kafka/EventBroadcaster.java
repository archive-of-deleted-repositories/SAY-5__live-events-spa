package com.say5.livevents.kafka;

import com.say5.livevents.domain.LiveEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * In-process fan-out for live events. The Kafka consumer pushes events here;
 * the SSE controller subscribes to the resulting {@link Flux}.
 *
 * Multicast with per-subscriber buffering: a slow consumer will not block the
 * Kafka thread. If a subscriber overflows its buffer it is dropped (FAIL_FAST
 * is intentionally avoided so one client cannot stop the broadcast).
 */
@Component
public class EventBroadcaster {

    private final Sinks.Many<LiveEvent> sink;

    public EventBroadcaster(@Value("${livevents.sse.backpressure-buffer:1024}") int bufferSize) {
        this.sink = Sinks.many().multicast().onBackpressureBuffer(bufferSize, false);
    }

    public void publish(LiveEvent event) {
        sink.tryEmitNext(event);
    }

    public Flux<LiveEvent> stream() {
        return sink.asFlux();
    }
}
