package com.example.booking.outbox;

import com.example.booking.shared.outbox.OutboxPublisher;
import com.example.booking.shared.time.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Polls {@code outbox_event} for unpublished rows, publishes a batch, and
 * marks successes / schedules retries / dead-letters failures. This is the
 * "read" side of the outbox — the {@code JdbcOutboxWriter} is the "write"
 * side.
 *
 * <p><strong>Ordering.</strong> The batch is sorted by
 * {@code (aggregate_id, aggregate_version)} so events from a single aggregate
 * reach the bus in order.
 *
 * <p><strong>Retry.</strong> A failed row gets exponential backoff
 * ({@code min(30s · 2^attempts, 1h)}); after {@code MAX_ATTEMPTS} it's marked
 * dead-letter for an operator to inspect — never silently dropped. Delivery
 * is at-least-once; consumers dedupe on the event id.
 *
 * <p><strong>Concurrency.</strong> The poll uses
 * {@code FOR UPDATE SKIP LOCKED}, so multiple dispatcher instances never
 * double-send a row. A single scheduled instance (as here) preserves
 * per-aggregate order trivially.
 */
public final class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    static final int BATCH_SIZE = 10;
    static final short MAX_ATTEMPTS = 10;
    static final Duration BASE_BACKOFF = Duration.ofSeconds(30);
    static final Duration MAX_BACKOFF = Duration.ofHours(1);

    private static final String POLL_SQL = """
        SELECT event_id, aggregate_type, aggregate_id, aggregate_version,
               event_type, event_schema_version, payload::text AS payload,
               occurred_at, attempts
        FROM outbox_event
        WHERE published_at   IS NULL
          AND dead_letter_at IS NULL
          AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
        ORDER BY aggregate_id, aggregate_version, created_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """;

    private static final String MARK_PUBLISHED_SQL = """
        UPDATE outbox_event SET published_at = :now, last_error = NULL
        WHERE event_id = :event_id
        """;

    private static final String SCHEDULE_RETRY_SQL = """
        UPDATE outbox_event
        SET attempts = :attempts, next_attempt_at = :next_attempt_at, last_error = :last_error
        WHERE event_id = :event_id
        """;

    private static final String DEAD_LETTER_SQL = """
        UPDATE outbox_event
        SET attempts = :attempts, dead_letter_at = :now, dead_letter_reason = :reason, last_error = :reason
        WHERE event_id = :event_id
        """;

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final OutboxPublisher publisher;
    private final Clock clock;
    private final MeterRegistry meters;

    public OutboxDispatcher(NamedParameterJdbcTemplate jdbc,
                            TransactionTemplate tx,
                            OutboxPublisher publisher,
                            Clock clock,
                            MeterRegistry meters) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.publisher = publisher;
        this.clock = clock;
        this.meters = meters;
    }

    @Scheduled(fixedDelayString = "${booking.outbox.dispatch-interval-ms:10000}")
    public void dispatch() {
        try {
            dispatchOnce();
        } catch (RuntimeException e) {
            // The scheduled runner must never die on a transient DB blip.
            log.error("Outbox dispatcher iteration failed — will retry on next schedule", e);
        }
    }

    /**
     * One polling batch inside a single transaction (so {@code SKIP LOCKED} is
     * honoured). Returns the number of rows processed so tests can assert
     * progress. Visible for testing.
     */
    public int dispatchOnce() {
        Instant now = clock.instant();
        Integer processed = tx.execute(status -> {
            List<OutboxRow> batch = poll(now);
            if (batch.isEmpty()) {
                return 0;
            }
            List<OutboxPublisher.PublishRequest> requests =
                batch.stream().map(OutboxDispatcher::toPublishRequest).toList();

            OutboxPublisher.PublishResult result;
            try {
                result = publisher.publish(requests);
            } catch (RuntimeException publisherFailure) {
                // The whole batch failed — schedule a retry for every row.
                log.warn("Publisher threw — treating batch as failed", publisherFailure);
                result = new OutboxPublisher.PublishResult(
                    Set.of(),
                    batch.stream().collect(java.util.stream.Collectors.toMap(
                        OutboxRow::eventId,
                        r -> publisherFailure.getClass().getSimpleName() + ": " + publisherFailure.getMessage(),
                        (a, b) -> a)));
            }
            applyOutcomes(batch, result, now);
            return batch.size();
        });
        return processed == null ? 0 : processed;
    }

    private List<OutboxRow> poll(Instant now) {
        return jdbc.query(POLL_SQL,
            new MapSqlParameterSource()
                .addValue("now", Timestamp.from(now))
                .addValue("limit", BATCH_SIZE),
            (rs, rowNum) -> new OutboxRow(
                (UUID) rs.getObject("event_id"),
                rs.getString("aggregate_type"),
                rs.getString("event_type"),
                rs.getString("payload"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getShort("attempts")));
    }

    private void applyOutcomes(List<OutboxRow> batch, OutboxPublisher.PublishResult result, Instant now) {
        Set<UUID> published = new HashSet<>(result.successful());
        List<UUID> dlqIds = new ArrayList<>();
        int retried = 0;

        for (OutboxRow row : batch) {
            if (published.contains(row.eventId())) {
                update(MARK_PUBLISHED_SQL, new MapSqlParameterSource()
                    .addValue("event_id", row.eventId())
                    .addValue("now", Timestamp.from(now)));
                continue;
            }
            String reason = result.failed().getOrDefault(row.eventId(), "no result returned by publisher");
            short nextAttempts = (short) (row.attempts() + 1);
            if (nextAttempts >= MAX_ATTEMPTS) {
                update(DEAD_LETTER_SQL, new MapSqlParameterSource()
                    .addValue("event_id", row.eventId())
                    .addValue("attempts", (int) nextAttempts)
                    .addValue("now", Timestamp.from(now))
                    .addValue("reason", "max attempts exceeded: " + reason));
                dlqIds.add(row.eventId());
            } else {
                update(SCHEDULE_RETRY_SQL, new MapSqlParameterSource()
                    .addValue("event_id", row.eventId())
                    .addValue("attempts", (int) nextAttempts)
                    .addValue("next_attempt_at", Timestamp.from(now.plus(backoff(nextAttempts))))
                    .addValue("last_error", reason));
                retried++;
            }
        }

        meters.counter("outbox.published").increment(batch.size() - retried - dlqIds.size());
        if (retried > 0) meters.counter("outbox.failed").increment(retried);
        if (!dlqIds.isEmpty()) {
            meters.counter("outbox.dlq").increment(dlqIds.size());
            log.warn("Dead-lettered outbox events: {}", dlqIds);
        }
    }

    /** {@code min(BASE · 2^(attempts-1), MAX)} — capped, overflow-guarded. */
    static Duration backoff(short attempts) {
        int shift = Math.min(attempts - 1, 20);
        Duration candidate;
        try {
            candidate = BASE_BACKOFF.multipliedBy(1L << shift);
        } catch (ArithmeticException overflow) {
            candidate = MAX_BACKOFF;
        }
        return candidate.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : candidate;
    }

    private void update(String sql, MapSqlParameterSource params) {
        jdbc.update(sql, params);
    }

    private static OutboxPublisher.PublishRequest toPublishRequest(OutboxRow row) {
        return new OutboxPublisher.PublishRequest(
            row.eventId(), row.aggregateType(), row.eventType(), row.payload(), row.occurredAt());
    }

    /** Package-private projection of an {@code outbox_event} row — only the columns the dispatcher needs. */
    record OutboxRow(
        UUID eventId,
        String aggregateType,
        String eventType,
        String payload,
        Instant occurredAt,
        short attempts
    ) {
    }
}
