package com.example.booking.shared.identity;

import java.util.Objects;
import java.util.UUID;

/** Typed id for a subscription. Shared-kernel value object (see {@code PlanId}). */
public record SubscriptionId(UUID value) {

    public SubscriptionId {
        Objects.requireNonNull(value, "SubscriptionId value must not be null");
    }

    public static SubscriptionId newId() {
        return new SubscriptionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
