package com.example.booking.shared.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Publishes a batch of outbox events to the integration bus. A port: the
 * concrete implementation decides transport details (a cloud event bus in
 * production, an in-memory fake in tests — see example 03); callers only see
 * this interface.
 */
public interface OutboxPublisher {

    PublishResult publish(Iterable<PublishRequest> batch);

    /**
     * The minimal shape the publisher needs — excludes dispatcher-private
     * state (attempts, next-attempt-at) and producer metadata the transport
     * doesn't surface.
     */
    record PublishRequest(
        UUID eventId,
        String aggregateType,
        String eventType,
        String payload,
        Instant occurredAt
    ) {
    }

    /**
     * Per-request outcome. The {@code successful} and {@code failed} keys are
     * disjoint and together cover every entry in the submitted batch, so the
     * dispatcher can mark each row precisely.
     */
    record PublishResult(Set<UUID> successful, Map<UUID, String> failed) {
    }
}
