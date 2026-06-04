package com.example.booking.shared.outbox;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.util.Map;

/**
 * JDBC-backed {@link OutboxWriter}. The INSERT participates in the caller's
 * transaction via Spring's transaction synchronization — no dual write, no
 * post-commit hook, no self-opened transaction. If the caller rolls back,
 * the outbox row disappears with the aggregate write. An integration test
 * pins exactly that rollback-parity property.
 *
 * <p>The caller opens the transaction (typically {@code @Transactional} on
 * the application service). If no transaction is active, the INSERT
 * auto-commits on its own connection — which is safe but breaks the
 * "atomic with the aggregate" invariant the pattern exists to provide.
 *
 * <p>({@code tools.jackson} is Jackson 3's package — this is a Spring Boot 4
 * codebase.)
 */
public final class JdbcOutboxWriter implements OutboxWriter {

    private static final String INSERT_SQL = """
        INSERT INTO outbox_event (
            event_id, aggregate_type, aggregate_id, aggregate_version,
            event_type, event_schema_version,
            payload, metadata,
            tenant_id, occurred_at
        ) VALUES (
            :event_id, :aggregate_type, :aggregate_id, :aggregate_version,
            :event_type, :event_schema_version,
            CAST(:payload AS JSONB), CAST(:metadata AS JSONB),
            :tenant_id, :occurred_at
        )
        """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcOutboxWriter(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void append(OutboxEvent event) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("event_id", event.eventId())
            .addValue("aggregate_type", event.aggregateType())
            .addValue("aggregate_id", event.aggregateId())
            .addValue("aggregate_version", event.aggregateVersion())
            .addValue("event_type", event.eventType())
            .addValue("event_schema_version", (int) event.eventSchemaVersion())
            .addValue("payload", event.payload())
            .addValue("metadata", objectMapper.writeValueAsString(event.metadata()))
            .addValue("tenant_id", event.tenantId().value())
            .addValue("occurred_at", Timestamp.from(event.occurredAt()));
        jdbc.update(INSERT_SQL, params);
    }
}
