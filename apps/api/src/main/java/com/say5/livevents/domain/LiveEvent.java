package com.say5.livevents.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event passed through the in-memory broadcast and the SSE stream.
 *
 * The {@code payload} field is stored as a raw JSON string. The producer is
 * trusted to emit valid JSON; the API treats it as opaque and forwards it
 * to consumers verbatim.
 */
public record LiveEvent(
        UUID id,
        Instant timestamp,
        EventType type,
        Severity severity,
        String source,
        String payload) {
}
