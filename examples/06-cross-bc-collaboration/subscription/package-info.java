/**
 * The subscription bounded context.
 *
 * <p>This is the declaration that authorizes the cross-context calls in
 * {@code SubscriptionServiceImpl}. The module may reference the shared kernel
 * and the <em>contract</em> surfaces of the plan and tenant contexts — and
 * nothing else. A reference to any other context, or to plan/tenant internals
 * rather than their contracts, fails the Modulith verification test
 * (example 04).
 *
 * <p>Note how ids ({@code PlanId}, {@code TenantId}) live in the shared kernel
 * rather than in plan/tenant contracts. That's deliberate: it lets the
 * {@code SubscriptionActivated} event carry those ids without this module —
 * or any consumer of the event — depending on the issuing context. It's the
 * trick that keeps the module graph acyclic.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {
        "shared",
        "plan :: contract",
        "tenant :: contract"
    }
)
package com.example.booking.subscription;
