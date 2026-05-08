package com.say5.livevents.api;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.say5.livevents.domain.LiveEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire format for a single event. {@code payload} is forwarded as raw JSON
 * so the client receives the producer's payload verbatim.
 */
public record EventDto(
        UUID id,
        Instant timestamp,
        String type,
        String severity,
        String source,
        @JsonRawValue String payload) {

    public static EventDto from(LiveEvent ev) {
        return new EventDto(
                ev.id(),
                ev.timestamp(),
                ev.type().name().toLowerCase(),
                ev.severity().name().toLowerCase(),
                ev.source(),
                ev.payload());
    }
}
