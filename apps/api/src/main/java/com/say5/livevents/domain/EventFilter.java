package com.say5.livevents.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * Server-side filter inputs for history queries and CSV export.
 *
 * Filters are conjunctive: every supplied field must match. Unset fields
 * are ignored.
 */
public record EventFilter(
        Optional<EventType> type,
        Optional<Severity> severity,
        Optional<Instant> from,
        Optional<Instant> to) {

    public static EventFilter parse(String type, String severity, String from, String to) {
        return new EventFilter(
                EventType.parse(type),
                Severity.parse(severity),
                parseInstant(from),
                parseInstant(to));
    }

    private static Optional<Instant> parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(raw.trim()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
