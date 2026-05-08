package com.say5.livevents;

import com.say5.livevents.domain.EventFilter;
import com.say5.livevents.domain.EventType;
import com.say5.livevents.domain.Severity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EventFilterTest {

    @Test
    void allEmptyFieldsParseToEmptyOptionals() {
        EventFilter f = EventFilter.parse(null, null, null, null);
        assertThat(f.type()).isEmpty();
        assertThat(f.severity()).isEmpty();
        assertThat(f.from()).isEmpty();
        assertThat(f.to()).isEmpty();
    }

    @Test
    void typeParsedCaseInsensitively() {
        assertThat(EventFilter.parse("OrDeR", null, null, null).type()).contains(EventType.ORDER);
    }

    @Test
    void severityParsedCaseInsensitively() {
        assertThat(EventFilter.parse(null, "warning", null, null).severity())
                .contains(Severity.WARNING);
    }

    @Test
    void invalidEnumValueIsDropped() {
        assertThat(EventFilter.parse("not-a-type", "nope", null, null).type()).isEmpty();
        assertThat(EventFilter.parse("not-a-type", "nope", null, null).severity()).isEmpty();
    }

    @Test
    void instantsParsedFromIso8601() {
        EventFilter f = EventFilter.parse(null, null, "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z");
        assertThat(f.from()).contains(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(f.to()).contains(Instant.parse("2026-02-01T00:00:00Z"));
    }

    @Test
    void invalidInstantsAreDropped() {
        EventFilter f = EventFilter.parse(null, null, "yesterday", "later");
        assertThat(f.from()).isEmpty();
        assertThat(f.to()).isEmpty();
    }
}
