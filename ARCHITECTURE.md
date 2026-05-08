# Architecture notes

## SSE protocol, reconnect, Last-Event-ID

The browser's `EventSource` reconnects automatically on disconnect, but the
defaults are not great:

- It does not back off on repeated failures, so a flapping server gets
  hammered.
- The default `retry` interval is implementation-defined (~3s in Chromium).
- `Last-Event-ID` is sent automatically as a header, but only if the server
  supplied an `id:` field on every event. Some servers forget.

This repo wraps `EventSource` in `apps/web/src/lib/sse.ts`:

1. Every reconnect runs through `backoffDelayMs(attempt)`, which doubles
   from 500ms up to a 30s ceiling. No jitter is added; the production
   topology assumes a CDN in front of the API absorbs reconnect bursts.
2. The most recent event id is tracked client-side. On reconnect the wrapper
   appends `?lastEventId=<id>` to the URL as well as letting the
   `Last-Event-ID` header flow naturally — the API uses whichever it gets.
3. Connection state (`open` / `error` / `closed`) is reported through
   `onStateChange`, so the UI's `LiveIndicator` reflects the truth instead
   of always showing "connected".

Server-side (`SseController`):

- On connect we replay the latest 100 events from the read-model so the
  client sees a populated list immediately. If `Last-Event-ID` is supplied
  and resolves to a known event, the replay is bounded by that timestamp
  to avoid re-emitting events the client already has.
- A 15-second comment heartbeat (`:keep-alive`) is interleaved so idle
  proxies don't close the connection.
- Live events are pulled from an in-memory `Sinks.Many` multicast sink with
  per-subscriber buffering. A slow client cannot block the Kafka thread; if
  it overflows its buffer it is dropped (broadcast continues).

## Kafka consumer + read-model split rationale

The Kafka topic is the durable event log. Spring Boot consumes it and does
two things atomically:

1. Persists a row to `events`. This row is what `GET /api/events` and the
   CSV export both query.
2. Publishes the same event into the broadcaster, which feeds connected
   SSE clients.

Why not stream straight from Kafka to clients? Two reasons:

- **Replay on connect.** A cold client needs to see the last N events, not
  just whatever happens to arrive after they connect. Pulling from a
  read-model is one indexed query; pulling the same data from Kafka means
  consumer-group bookkeeping and offset seeking per client.
- **Querying.** Cursor pagination, filter-by-type, filter-by-severity, and
  time-range queries are all relational queries with indexes. Kafka is a
  log, not a query engine.

The read-model is intentionally minimal: a single `events` table with a
descending-timestamp index. Adding read-side projections (per-type
counters, per-source aggregates) would be additional Flyway migrations and
projection code; nothing about the topology blocks it.

## CSV export — chunked, no in-memory buffer

`/api/events/export.csv` paginates through the read-model in 500-row pages
and writes each row to the response stream as it goes. The HTTP response
uses `Transfer-Encoding: chunked` (the servlet container's default for
streamed `OutputStream` writes). A 1M-row export uses bounded memory: one
page at a time plus a small writer buffer.

CSV escaping follows RFC 4180: fields containing commas, double-quotes,
carriage returns, or newlines are wrapped in double-quotes; embedded
double-quotes are doubled.

## Mobile-responsive layout

Tailwind breakpoints are used directly:

- Default (mobile): event rows collapse to one column (timestamp, then
  type, severity, source, payload stacked vertically). The filter bar
  wraps onto multiple rows.
- `sm:` (>= 640px): rows lay out in a 5-column grid; the filter bar fits
  on one row.
- The `LiveIndicator` is in the page header at every viewport so the user
  always sees connection state.

There is no JS-driven media-query branching; everything is CSS. That keeps
the bundle small and avoids hydration-mismatch concerns (this is a static
SPA, so technically not relevant, but the discipline is the same).

## What's deliberately not here

- **Auth.** The product spec is single-user. Adding auth means adding
  identity, session, an authorization model, and tests for all of them.
- **Rate limiting on SSE.** A reverse proxy (nginx, Front Door, Cloudflare)
  is where this belongs. The app shouldn't reinvent it.
- **Per-tenant filtering.** No tenancy column on `events`, no user model.
- **Backpressure into Kafka.** If the broadcaster's buffer overflows, the
  affected client is dropped; the consumer keeps going. There is no
  feedback into the Kafka consumer's pause/resume API.
- **Schema registry.** Events are JSON strings. A production system should
  use Avro or Protobuf with a registry; that's out of scope for the
  portfolio target.
- **Distributed tracing.** Spring Boot Actuator is on, but there's no
  OpenTelemetry exporter wired up. The Bicep stub allocates an
  Application Insights resource as the eventual sink.
