package com.example.booking.subscription;

import com.example.booking.plan.contract.PlanServiceStub;
import com.example.booking.shared.identity.PlanId;
import com.example.booking.shared.identity.TenantId;
import com.example.booking.shared.money.Money;
import com.example.booking.subscription.contract.PlanNotActiveException;
import com.example.booking.subscription.contract.SubscriptionDto;
import com.example.booking.subscription.contract.SubscriptionStatus;
import com.example.booking.subscription.contract.TenantAlreadyHasSubscriptionException;
import com.example.booking.subscription.contract.TenantNotFoundException;
import com.example.booking.tenant.contract.TenantDto;
import com.example.booking.tenant.contract.TenantServiceStub;
import com.example.booking.testing.time.FakeClock;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A story test for the subscription context — and the whole point of the
 * cross-context convention. The real {@code SubscriptionServiceImpl} is wired
 * with the <em>stubs</em> the tenant and plan contexts publish in their
 * {@code contract} packages. No tenant module, no plan module, no Spring — yet
 * the cross-context validation is exercised for real.
 *
 * <p>This is only possible because the dependency runs through the facade.
 * Had the subscription service reached into the tenant repository, this test
 * would need a database (or a mock of an internal type). Through the contract,
 * a hand-written stub is enough.
 */
class SubscriptionServiceTest {

    private final FakeClock clock = FakeClock.startingAt(Instant.parse("2026-04-20T10:00:00Z"));
    private final TenantServiceStub tenants = new TenantServiceStub();
    private final PlanServiceStub plans = new PlanServiceStub();
    private final InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
    private final SubscriptionServiceImpl service =
        new SubscriptionServiceImpl(repository, tenants, plans, clock);

    @Test
    void subscribe_validates_both_contexts_then_activates() {
        TenantDto tenant = tenants.registerTenant("Acme", "acme-corp", "admin@acme.example");
        PlanId planId = plans.seedActive("Starter", Money.of("49.00", "EUR"));

        SubscriptionDto subscription = service.subscribe(tenant.id(), planId);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.status());
        assertEquals(tenant.id(), subscription.tenantId());
        assertEquals(planId, subscription.planId());
    }

    @Test
    void subscribe_rejects_an_unknown_tenant() {
        PlanId planId = plans.seedActive("Starter", Money.of("49.00", "EUR"));

        assertThrows(TenantNotFoundException.class,
            () -> service.subscribe(TenantId.newId(), planId));
    }

    @Test
    void subscribe_rejects_an_inactive_or_missing_plan() {
        TenantDto tenant = tenants.registerTenant("Acme", "acme-corp", "admin@acme.example");

        // A plan id the stub never seeded → existsAndActive == false.
        assertThrows(PlanNotActiveException.class,
            () -> service.subscribe(tenant.id(), PlanId.newId()));
    }

    @Test
    void subscribe_rejects_a_second_subscription_for_the_same_tenant() {
        TenantDto tenant = tenants.registerTenant("Acme", "acme-corp", "admin@acme.example");
        PlanId planId = plans.seedActive("Starter", Money.of("49.00", "EUR"));
        service.subscribe(tenant.id(), planId);

        assertThrows(TenantAlreadyHasSubscriptionException.class,
            () -> service.subscribe(tenant.id(), planId));
    }
}
