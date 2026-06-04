package com.example.booking.tenant;

import com.example.booking.shared.identity.TenantId;

import java.util.Objects;
import java.util.UUID;

/**
 * Internal wrapper the tenant aggregate emits via {@code @DomainEvents}. It
 * carries the outbox-row identity ({@code aggregateType} / {@code aggregateId}
 * / {@code aggregateVersion}) explicitly.
 *
 * <p>Why bother instead of emitting the bare event? The {@code outbox_event}
 * table has a unique key on {@code (aggregate_type, aggregate_id,
 * aggregate_version)} to make writes idempotent. The tenant aggregate's own
 * events key on the aggregate's optimistic-lock version; but an insert-only
 * sibling row (an acceptance record, an audit entry) writing in the same
 * transaction would collide at "version 0". Giving siblings a surrogate
 * aggregate identity via {@link #forSibling} keeps every row's key distinct.
 */
record TenantOutboxEnvelope(
    String aggregateType,
    String aggregateId,
    long aggregateVersion,
    Object event
) {
    TenantOutboxEnvelope {
        Objects.requireNonNull(aggregateType, "aggregateType");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(event, "event");
    }

    /** The tenant aggregate's own events — keyed on its optimistic-lock version. */
    static TenantOutboxEnvelope forTenant(long aggregateVersion, TenantId tenantId, Object event) {
        return new TenantOutboxEnvelope("Tenant", tenantId.value().toString(), aggregateVersion, event);
    }

    /** An insert-only sibling row — uses the row's own UUID so its outbox key never collides. */
    static TenantOutboxEnvelope forSibling(String aggregateType, UUID rowId, Object event) {
        return new TenantOutboxEnvelope(aggregateType, rowId.toString(), 0L, event);
    }
}
