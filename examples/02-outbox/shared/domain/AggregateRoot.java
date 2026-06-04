package com.example.booking.shared.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base class for aggregates that emit integration events.
 *
 * <p>Two emission styles coexist in the codebase. A Spring Data JDBC
 * aggregate (like {@code Tenant} in example 01) can use the framework's
 * {@code @DomainEvents} hook. An aggregate persisted by a hand-written
 * repository extends this base instead: it calls {@link #emit(Object)} from
 * its command methods, and the repository drains {@link #pullPendingEvents()}
 * into the outbox during {@code save()}. Either way, the rule is the same —
 * <strong>the aggregate never touches the outbox directly</strong>, and the
 * event is written in the same transaction as the state change.
 *
 * <p>Not thread-safe — aggregates are never shared across threads. Each
 * request loads, mutates, saves, and discards its own instance.
 */
public abstract class AggregateRoot {

    private final List<Object> pendingEvents = new ArrayList<>();

    protected final void emit(Object event) {
        Objects.requireNonNull(event, "event");
        pendingEvents.add(event);
    }

    /** Non-consuming snapshot — handy for tests that assert on emitted events. */
    public final List<Object> pendingEvents() {
        return List.copyOf(pendingEvents);
    }

    /**
     * Drain and return the buffered events. The repository calls this inside
     * {@code save()} and forwards the result to the outbox writer; clearing
     * the buffer means a second save can't re-emit the same events.
     */
    public final List<Object> pullPendingEvents() {
        List<Object> drained = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return drained;
    }
}
