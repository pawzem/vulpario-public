package com.example.booking.subscription.contract;

import com.example.booking.shared.identity.TenantId;

/** Thrown when a tenant that already has a subscription tries to take another. */
public class TenantAlreadyHasSubscriptionException extends IllegalStateException {

    public TenantAlreadyHasSubscriptionException(TenantId tenantId) {
        super("tenant already has a subscription: " + tenantId);
    }
}
