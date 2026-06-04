package com.example.booking.shared.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * A typed identifier for a tenant. A shared-kernel value object — in the
 * real layout it lives in {@code com.example.booking.shared.identity} and is
 * reused by every bounded context (it's how an event can carry a tenant id
 * without dragging the tenant module into the consumer's dependency graph).
 * It's included alongside this example so the code reads top-to-bottom.
 *
 * <p>Wrapping the {@link UUID} in a record turns a thousand interchangeable
 * {@code UUID} parameters into compiler-checked, intention-revealing types:
 * you can't pass a {@code PlanId} where a {@code TenantId} is expected.
 */
public record TenantId(UUID value) {

    public TenantId {
        Objects.requireNonNull(value, "TenantId value must not be null");
    }

    public static TenantId of(UUID value) {
        return new TenantId(value);
    }

    public static TenantId of(String value) {
        return new TenantId(UUID.fromString(value));
    }

    public static TenantId newId() {
        return new TenantId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
