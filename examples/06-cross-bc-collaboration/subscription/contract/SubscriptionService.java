package com.example.booking.subscription.contract;

import com.example.booking.shared.identity.PlanId;
import com.example.booking.shared.identity.TenantId;

import java.util.Optional;

/**
 * The subscription context's facade. Its implementation collaborates with the
 * tenant and plan contexts — but only through <em>their</em> facades, and only
 * because this module declares those dependencies (see
 * {@code subscription/package-info.java}).
 */
public interface SubscriptionService {

    /**
     * Subscribe a tenant to a plan.
     *
     * @throws TenantNotFoundException if the tenant doesn't exist
     *         (validated via the tenant facade).
     * @throws PlanNotActiveException if the plan is missing or inactive
     *         (validated via the plan facade).
     * @throws TenantAlreadyHasSubscriptionException if the tenant is already
     *         subscribed.
     */
    SubscriptionDto subscribe(TenantId tenantId, PlanId planId);

    /** The tenant's current subscription, if any. */
    Optional<SubscriptionDto> findCurrentForTenant(TenantId tenantId);
}
