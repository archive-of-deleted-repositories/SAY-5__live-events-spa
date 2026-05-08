package com.say5.livevents.store;

import com.say5.livevents.domain.EventType;
import com.say5.livevents.domain.LiveEvent;
import com.say5.livevents.domain.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    private UUID id;

    @Column(name = "ts", nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Severity severity;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    public EventEntity() {
    }

    public EventEntity(UUID id, Instant timestamp, EventType type, Severity severity,
                       String source, String payload) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.severity = severity;
        this.source = source;
        this.payload = payload;
    }

    public static EventEntity from(LiveEvent e) {
        return new EventEntity(e.id(), e.timestamp(), e.type(), e.severity(), e.source(), e.payload());
    }

    public LiveEvent toDomain() {
        return new LiveEvent(id, timestamp, type, severity, source, payload);
    }

    public UUID getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public EventType getType() {
        return type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getSource() {
        return source;
    }

    public String getPayload() {
        return payload;
    }
}
