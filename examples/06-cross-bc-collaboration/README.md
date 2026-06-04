# 06 · Cross-context collaboration

Contexts have to work together — a subscription needs a real tenant and an
active plan. The rule: a context collaborates with another **only through its
published `contract`**, and only because it **declares the dependency**. Both
halves are checked on every build.

## What to look at

```
shared/money/Money.java                              shared-kernel value object
shared/identity/{PlanId,SubscriptionId}.java         shared-kernel ids (the acyclic-graph trick)
plan/contract/PlanService.java, PlanServiceStub.java the plan facade + its published stub
subscription/
├── contract/SubscriptionService.java + DTOs/events/exceptions
├── SubscriptionServiceImpl.java                     ← collaborates via TenantService + PlanService
├── package-info.java                                ← declares the allowed dependencies
└── SubscriptionServiceTest.java                     ← wired with both contexts' stubs
```

## The collaboration, in one constructor

```java
SubscriptionServiceImpl(SubscriptionRepository subscriptions,
                        TenantService tenants,   // ← sibling facade, not its repo
                        PlanService plans,        // ← sibling facade, not its aggregate
                        Clock clock)
```

`subscribe(tenantId, planId)` validates the tenant exists (`tenants.exists`)
and the plan is active (`plans.existsAndActive`) — entirely through the
facades. It never imports `Tenant` or `Plan` or touches their tables.

## Two halves, both enforced

1. **Through the contract.** The impl imports
   `com.example.booking.tenant.contract.TenantService`, never
   `com.example.booking.tenant.Tenant`. The aggregate isn't even visible
   (it's package-private — example 01).
2. **Declared and verified.** `subscription/package-info.java` lists
   `allowedDependencies = { "plan :: contract", "tenant :: contract" }`. Add a
   dependency in code without declaring it here — or depend on internals
   rather than a contract — and `ApplicationModulesTest` (example 04) fails
   the build.

## The acyclic-graph trick

`SubscriptionActivated` carries a `TenantId` and a `PlanId`. If those ids
lived in the tenant/plan contracts, every consumer of the event would depend
on the tenant and plan modules — and `tenant → subscription → tenant` cycles
would appear fast. Instead the ids live in the **shared kernel**
(`shared/identity/`), so an event can carry an id without coupling the
consumer to the issuer. Where you put your id types decides whether your
module graph stays a DAG.

## The payoff: testing across a boundary with no boundary

`SubscriptionServiceTest` wires the real subscription service against
`TenantServiceStub` and `PlanServiceStub` — the stubs each context ships in
its `contract` package. No tenant module, no plan module, no database, no
Spring; the cross-context rules are still exercised for real. That's only
possible because the dependency goes through the facade. Reach past it, and
you'd need the whole module (or a mock of a type you shouldn't see).

> The in-process re-publish that lets a sibling context's policy react to
> `SubscriptionActivated` is shown in example 02 (`TenantOutboxListener`). Same
> mechanism here.
