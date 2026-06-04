package com.example.booking.shared.outbox;

import com.example.booking.shared.identity.TenantId;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A single append-only outbox row. Written in the same transaction as the
 * aggregate state change and later published to the message bus by the
 * {@code OutboxDispatcher}.
 *
 * <p>{@code payload} is a CloudEvents-shaped JSON string produced by the
 * owning context; {@code metadata} carries correlation/causation ids so a
 * saga can be stitched back together from logs and traces. {@code tenantId}
 * is denormalized onto every row for routing and filtering.
 */
public record OutboxEvent(
    UUID eventId,
    String aggregateType,
    String aggregateId,
    long aggregateVersion,
    String eventType,
    short eventSchemaVersion,
    String payload,
    Map<String, Object> metadata,
    TenantId tenantId,
    Instant occurredAt
) {

    public static final String META_CORRELATION_ID = "correlation_id";
    public static final String META_CAUSATION_ID = "causation_id";

    public OutboxEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(aggregateType, "aggregateType");
        Objects.requireNonNull(aggregateId, "aggregateId");
        if (aggregateVersion < 0) {
            throw new IllegalArgumentException("aggregateVersion must be >= 0: " + aggregateVersion);
        }
        Objects.requireNonNull(eventType, "eventType");
        if (eventSchemaVersion < 1) {
            throw new IllegalArgumentException("eventSchemaVersion must be >= 1: " + eventSchemaVersion);
        }
        Objects.requireNonNull(payload, "payload");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
