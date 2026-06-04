package com.example.booking.subscription;

import com.example.booking.plan.contract.PlanService;
import com.example.booking.shared.identity.PlanId;
import com.example.booking.shared.identity.SubscriptionId;
import com.example.booking.shared.identity.TenantId;
import com.example.booking.shared.time.Clock;
import com.example.booking.subscription.contract.PlanNotActiveException;
import com.example.booking.subscription.contract.SubscriptionDto;
import com.example.booking.subscription.contract.SubscriptionService;
import com.example.booking.subscription.contract.TenantAlreadyHasSubscriptionException;
import com.example.booking.subscription.contract.TenantNotFoundException;
import com.example.booking.tenant.contract.TenantService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * The cross-context collaboration, in one constructor.
 *
 * <p>{@code SubscriptionServiceImpl} depends on the <strong>services</strong>
 * of two sibling contexts — {@link TenantService} and {@link PlanService} —
 * never their repositories or aggregates. In production Spring injects the
 * real impls; in tests you pass {@code TenantServiceStub} and
 * {@code PlanServiceStub} (the stubs each context publishes in its
 * {@code contract} package). See {@code SubscriptionServiceTest}.
 *
 * <p>This dependency is declared in {@code subscription/package-info.java} as
 * {@code allowedDependencies = { "plan :: contract", "tenant :: contract" }}.
 * If this class tried to import {@code com.example.booking.tenant.Tenant} (the
 * aggregate) instead of the contract, the Modulith fitness test (example 04)
 * would fail the build — the boundary isn't a guideline, it's checked.
 */
class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptions;
    private final TenantService tenants;
    private final PlanService plans;
    private final Clock clock;

    SubscriptionServiceImpl(SubscriptionRepository subscriptions,
                            TenantService tenants,
                            PlanService plans,
                            Clock clock) {
        this.subscriptions = Objects.requireNonNull(subscriptions);
        this.tenants = Objects.requireNonNull(tenants);
        this.plans = Objects.requireNonNull(plans);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional
    public SubscriptionDto subscribe(TenantId tenantId, PlanId planId) {
        // Validate across contexts — but only through their facades.
        if (!tenants.exists(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
        if (!plans.existsAndActive(planId)) {
            throw new PlanNotActiveException(planId);
        }
        if (subscriptions.findCurrentForTenant(tenantId).isPresent()) {
            throw new TenantAlreadyHasSubscriptionException(tenantId);
        }

        Subscription subscription = Subscription.activate(
            SubscriptionId.newId(), tenantId, planId, clock.instant());
        return subscriptions.save(subscription).toDto();
    }

    @Override
    public Optional<SubscriptionDto> findCurrentForTenant(TenantId tenantId) {
        return subscriptions.findCurrentForTenant(tenantId).map(Subscription::toDto);
    }
}
