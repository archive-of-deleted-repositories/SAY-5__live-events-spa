package com.say5.livevents.api;

import com.say5.livevents.domain.EventFilter;
import com.say5.livevents.domain.LiveEvent;
import com.say5.livevents.store.EventQueryService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventQueryService queryService;

    public EventController(EventQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public HistoryResponse history(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "50") int limit,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        EventFilter filter = EventFilter.parse(type, severity, from, to);
        Instant cursorTs = parseCursor(cursor);
        List<LiveEvent> events = queryService.history(filter, cursorTs, limit);
        String nextCursor = events.isEmpty()
                ? null
                : events.get(events.size() - 1).timestamp().toString();
        return new HistoryResponse(events.stream().map(EventDto::from).toList(), nextCursor);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public void exportCsv(
            HttpServletResponse response,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) throws IOException {
        response.setContentType("text/csv; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"events.csv\"");
        EventFilter filter = EventFilter.parse(type, severity, from, to);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write("id,timestamp,type,severity,source,payload");
            writer.newLine();
            queryService.exportStream(filter, ev -> writeRow(writer, ev));
            writer.flush();
        }
    }

    private static void writeRow(BufferedWriter writer, LiveEvent ev) {
        try {
            writer.write(CsvEscape.row(
                    ev.id().toString(),
                    ev.timestamp().toString(),
                    ev.type().name().toLowerCase(),
                    ev.severity().name().toLowerCase(),
                    ev.source(),
                    ev.payload()));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Instant parseCursor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid cursor: " + raw);
        }
    }
}
