package com.say5.livevents.api;

import com.say5.livevents.domain.EventFilter;
import com.say5.livevents.domain.LiveEvent;
import com.say5.livevents.kafka.EventBroadcaster;
import com.say5.livevents.store.EventQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-Sent Events endpoint. Each connection receives:
 *   1. an initial replay of the latest N persisted events (newest-first
 *      reversed to chronological order so the client can append),
 *   2. all subsequent live events from {@link EventBroadcaster},
 *   3. a periodic comment heartbeat to keep idle proxies from closing the
 *      connection.
 *
 * If the client supplies {@code Last-Event-ID} (an event UUID), we use the
 * timestamp of that event to bound the replay so reconnects don't duplicate
 * already-seen events.
 */
@RestController
public class SseController {

    private final EventBroadcaster broadcaster;
    private final EventQueryService queryService;
    private final int historyOnConnect;

    public SseController(EventBroadcaster broadcaster,
                         EventQueryService queryService,
                         @Value("${livevents.sse.history-on-connect:100}") int historyOnConnect) {
        this.broadcaster = broadcaster;
        this.queryService = queryService;
        this.historyOnConnect = historyOnConnect;
    }

    @GetMapping(value = "/api/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<EventDto>> stream(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        Instant resumeFrom = resolveResumeCursor(lastEventId);
        List<LiveEvent> backlog = queryService.history(
                EventFilter.parse(null, null, null, null),
                null,
                historyOnConnect);

        List<LiveEvent> ordered = backlog.stream()
                .filter(e -> resumeFrom == null || e.timestamp().isAfter(resumeFrom))
                .sorted((a, b) -> a.timestamp().compareTo(b.timestamp()))
                .toList();

        Flux<ServerSentEvent<EventDto>> replay = Flux.fromIterable(ordered).map(SseController::sse);
        Flux<ServerSentEvent<EventDto>> live = broadcaster.stream().map(SseController::sse);
        Flux<ServerSentEvent<EventDto>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(i -> ServerSentEvent.<EventDto>builder()
                        .comment("keep-alive")
                        .build());

        return Flux.merge(replay.concatWith(live), heartbeat);
    }

    private Instant resolveResumeCursor(String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return null;
        }
        try {
            UUID id = UUID.fromString(lastEventId.trim());
            return queryService.findById(id).map(LiveEvent::timestamp).orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ServerSentEvent<EventDto> sse(LiveEvent ev) {
        return ServerSentEvent.<EventDto>builder()
                .id(ev.id().toString())
                .event("event")
                .data(EventDto.from(ev))
                .build();
    }

    // Only present so the field isn't flagged unused in some IDE configs.
    @SuppressWarnings("unused")
    private static Optional<UUID> _unused() {
        return Optional.empty();
    }
}
