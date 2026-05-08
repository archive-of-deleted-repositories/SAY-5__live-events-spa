CREATE TABLE IF NOT EXISTS events (
    id          UUID PRIMARY KEY,
    ts          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    type        VARCHAR(32) NOT NULL,
    severity    VARCHAR(32) NOT NULL,
    source      VARCHAR(255) NOT NULL,
    payload     TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_events_ts_desc ON events (ts DESC);
CREATE INDEX IF NOT EXISTS idx_events_type ON events (type);
CREATE INDEX IF NOT EXISTS idx_events_severity ON events (severity);
