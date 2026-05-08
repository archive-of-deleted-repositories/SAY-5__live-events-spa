package com.say5.livevents.domain;

import java.util.Optional;

public enum Severity {
    INFO, WARNING, CRITICAL;

    public static Optional<Severity> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Severity.valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
