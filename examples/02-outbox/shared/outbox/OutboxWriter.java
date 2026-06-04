package com.example.booking.shared.outbox;

/**
 * Writes an {@link OutboxEvent} in the same JDBC transaction as the aggregate
 * state change. No post-commit hooks, no dual writes — the writer
 * <strong>must</strong> participate in the caller's transaction. That single
 * rule is the whole point of the transactional outbox: the event and the
 * state it describes commit or roll back together.
 */
public interface OutboxWriter {

    void append(OutboxEvent event);
}
