package com.say5.livevents.domain;

import java.util.Optional;

public enum EventType {
    ORDER, PAYMENT, SHIPMENT, ALERT;

    public static Optional<EventType> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(EventType.valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
