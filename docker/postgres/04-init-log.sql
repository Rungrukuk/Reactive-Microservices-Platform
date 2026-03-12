CREATE DATABASE log;
GRANT ALL PRIVILEGES ON DATABASE log TO platform;
\c log;

CREATE TABLE monitoring_events (
    id            BIGSERIAL    PRIMARY KEY,
    event_type    VARCHAR(100) NOT NULL,
    service_name  VARCHAR(100) NOT NULL,
    user_id       VARCHAR(36),
    user_agent    VARCHAR(255),
    client_city   VARCHAR(100),
    details       TEXT,
    metadata      JSONB,
    timestamp     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_monitoring_events_event_type ON monitoring_events(event_type);
CREATE INDEX idx_monitoring_events_service_name ON monitoring_events(service_name);
CREATE INDEX idx_monitoring_events_user_id ON monitoring_events(user_id);
CREATE INDEX idx_monitoring_events_timestamp ON monitoring_events(timestamp DESC);
