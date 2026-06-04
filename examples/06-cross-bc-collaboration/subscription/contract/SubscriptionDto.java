package com.example.booking.subscription.contract;

import com.example.booking.shared.identity.PlanId;
import com.example.booking.shared.identity.SubscriptionId;
import com.example.booking.shared.identity.TenantId;

import java.util.Objects;

/** Read-side view of a subscription, returned by {@link SubscriptionService}. */
public record SubscriptionDto(
    SubscriptionId id,
    TenantId tenantId,
    PlanId planId,
    SubscriptionStatus status
) {
    public SubscriptionDto {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(status, "status");
    }
}
