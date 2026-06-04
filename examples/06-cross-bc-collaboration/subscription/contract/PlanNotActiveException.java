package com.example.booking.subscription.contract;

import com.example.booking.shared.identity.PlanId;

/** Thrown when subscribing to a plan that's missing or inactive. */
public class PlanNotActiveException extends IllegalStateException {

    public PlanNotActiveException(PlanId planId) {
        super("plan is not active or does not exist: " + planId);
    }
}
