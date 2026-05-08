# live-events-spa

A React/TypeScript SPA that receives a live feed of domain events over
Server-Sent Events from a Spring Boot backend, with a Kafka topic as the
upstream source of truth and Postgres as the read-model. Events stream into
the UI in real time, can be filtered and searched client-side, and exported
to CSV through a streaming server endpoint.

## What this studies

- **SSE vs WebSocket.** SSE is one-way and runs over plain HTTP, so it
  cooperates with HTTP/2 multiplexing, browser proxies, and the native
  `EventSource` retry-on-disconnect with `Last-Event-ID`. WebSocket is
  bidirectional with lower per-message overhead but needs upgrade handling
  and a custom reconnect/resume protocol. The event feed in this app is
  one-way (server-to-client), which makes SSE the right fit.
- **Kafka as event source vs database as source of truth.** The Kafka topic
  is the durable event log; Postgres is a read-model populated by the
  consumer. This split means the API can serve cursor-paginated history
  without seeking on Kafka, and the read-model can be rebuilt by replaying
  from a configured offset.
- **Client-side and server-side filtering.** Live events filter
  client-side over the in-memory ring buffer (no round-trips while typing).
  History queries and CSV exports filter server-side so we don't pull rows
  the user isn't interested in. The same filter shape (`type`, `severity`,
  `from`, `to`, free-text) is honoured both ways.

## Modules

| Path                  | What it is                                                       |
| --------------------- | ---------------------------------------------------------------- |
| `apps/web/`           | Vite + React 18 + Redux Toolkit + RTK Query + Tailwind SPA       |
| `apps/api/`           | Spring Boot 3, JPA, Flyway, spring-kafka, SSE controller         |
| `infra/azure/`        | Bicep stub for AKS + Event Hubs + Postgres + Static Web Apps     |
| `scripts/`            | Helper scripts (event seeder)                                    |
| `docker-compose.yml`  | Full local stack: zookeeper, kafka, postgres, api, web           |
| `.github/workflows/`  | CI: lint/typecheck/test for both apps + Playwright e2e + builds  |

## Quickstart

```bash
make up           # docker compose up -d (zk + kafka + postgres + api + web)
make seed-events  # POST 25 synthetic events to /api/events/_seed
open http://localhost:5173
```

To run the apps individually:

```bash
# API (needs Java 21 + Maven; assumes Postgres + Kafka are reachable)
cd apps/api && mvn spring-boot:run

# Web (needs Node 20 + pnpm)
cd apps/web && pnpm install && pnpm dev
```

Tests:

```bash
cd apps/api && mvn -B test                 # JUnit + AssertJ
cd apps/web && pnpm test                   # Vitest + jsdom + RTL
cd apps/web && pnpm exec playwright test   # full e2e against the compose stack
```

The Kafka + Postgres testcontainers integration test (`KafkaSseIT`) only
runs when the `RUN_TESTCONTAINERS` environment variable is set; CI sets it.
Local runs without a reachable Docker daemon skip it cleanly.

## Architecture

```
  +---------+     events.live      +-----------+     +-----------+
  |producer | -------------------> |  Kafka    | --> |  Spring   |
  +---------+    JSON records      +-----------+     |  Boot     |
                                                     |  consumer |
                                                     +-----+-----+
                                                           |
                                          +----------------+----------------+
                                          |                                 |
                                  +-------v--------+                +-------v-------+
                                  |   Postgres     |                |  in-memory    |
                                  |  read-model    |                |  Sinks.Many<> |
                                  +-------+--------+                +-------+-------+
                                          |                                 |
                                  +-------v---------+              +--------v---------+
                                  | GET /api/events |              | GET /api/events  |
                                  | cursor history  |              |   /stream  (SSE) |
                                  | export.csv      |              |  Last-Event-ID   |
                                  +--------+--------+              +--------+---------+
                                           |                                |
                                           +---------------+----------------+
                                                           |
                                                  +--------v--------+
                                                  | React + Redux   |
                                                  | Toolkit + RTK Q |
                                                  | virtuoso list   |
                                                  +-----------------+
```

## What this is *not*

- **No real Azure.** `infra/azure/main.bicep` is documentation; nothing in
  CI provisions or validates against an Azure subscription.
- **No authentication.** Single-user, no login, no per-user state.
- **No Kafka admin UI.** Use Confluent Control Center or `kcat`
  externally if you need to inspect the topic.
- **No offline mode.** If the SSE connection drops, reconnect uses
  `Last-Event-ID` to resume; events that are evicted from the read-model
  before reconnect are gone.
- **No multi-tenant filtering.** All clients see all events.
- **No rate limiting on the SSE endpoint.** A reverse proxy is the right
  place to add this.
- **No historical replay beyond the read-model retention.** Events older
  than what's in Postgres are not served. Production would either keep
  Postgres rows longer or replay from the Kafka topic offset.
- **No Kubernetes manifests in this repo.** See `SAY-5/kube-deploy` for a
  Kubernetes + Terraform deployment pattern; this repo intentionally
  stops at "what would deploy" (the Bicep stub).

## License

[MIT](./LICENSE).
