package com.example.booking.subscription.contract;

import com.example.booking.shared.identity.PlanId;
import com.example.booking.shared.identity.SubscriptionId;
import com.example.booking.shared.identity.TenantId;

import java.time.Instant;
import java.util.Objects;

/**
 * Integration event: a tenant became subscribed to a plan. Note it carries
 * {@link TenantId} and {@link PlanId} — both shared-kernel types — so a
 * consumer (say, the directory context that lists active tenants) can react
 * without depending on either the tenant or plan module.
 */
public record SubscriptionActivated(
    SubscriptionId subscriptionId,
    TenantId tenantId,
    PlanId planId,
    Instant activatedAt
) {

    public static final String EVENT_TYPE = "subscription.SubscriptionActivated.v1";

    public SubscriptionActivated {
        Objects.requireNonNull(subscriptionId, "subscriptionId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(activatedAt, "activatedAt");
    }
}
