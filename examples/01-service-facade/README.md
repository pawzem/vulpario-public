# 01 · Service facade — one public surface per bounded context

The unit of modularity. Every bounded context exposes **exactly one public
interface** (`XxxService`) and keeps everything else package-private. This is
the convention the whole system is built on; the rest of the examples are
variations on it.

## What to look at

```
shared/identity/TenantId.java        a typed id (shared-kernel value object)
tenant/
├── contract/                        ← the ONLY public package
│   ├── package-info.java                @NamedInterface("contract")
│   ├── TenantService.java               the facade: commands + queries
│   ├── TenantServiceStub.java           in-memory impl for other modules' tests
│   ├── TenantDto.java                    read-side view returned by the service
│   ├── TenantStatus.java
│   ├── TenantRegistered.java            integration events
│   ├── TenantRenamed.java
│   ├── RegisterTenantRequest.java       HTTP wire records
│   ├── TenantResponse.java
│   └── TenantSlugAlreadyTakenException.java
├── Tenant.java                      ← aggregate            (package-private)
├── TenantServiceImpl.java           ← @Transactional impl  (package-private)
├── TenantRepository.java            ← Spring Data JDBC      (package-private)
├── TenantController.java            ← REST transport        (package-private)
└── package-info.java                   @ApplicationModule(allowedDependencies)
```

## The ideas

**One facade, and it's also the read model.** Commands (`registerTenant`,
`rename`, `suspend`) and queries (`findById`, `findAll`) live on the same
`TenantService`. There's no separate read-model interface or view-projection
type — the service is the read side. One thing to learn, one thing to stub.

**Default-closed visibility.** The aggregate, repository, impl, and
controller are all package-private. A consumer literally cannot name
`Tenant` or `TenantRepository`. Things get promoted to `public` only when a
test outside the package proves it needs them. This is what makes the
boundary real rather than aspirational — and it's why an AI editing a sibling
module *can't* accidentally couple to tenant internals: the types aren't on
its menu.

**Rich aggregate, no setters.** `Tenant` changes state through command
methods that enforce invariants and emit events — never `setName(...)`. A
fitness test (example 04) fails the build on any JavaBean setter on an
aggregate.

**Controllers are transport only.** `TenantController` binds the request,
calls one service method, maps the DTO to a wire record, returns. If a
response needed a field from another context, the *service* would source it
and return a complete DTO — the controller stays a one-liner per route.

**The stub ships in `contract/`.** `TenantServiceStub` is how *other*
modules' unit tests use the tenant context without booting it — the
no-Mockito convention (example 05). It's a real implementation the compiler
keeps honest.

## Why this matters for AI-assisted work

Every one of these is a constraint that shrinks the space of "plausible but
wrong". A model asked to add a feature to the subscription context sees a
`TenantService` interface and a stub — not a sprawling package of mutable
aggregates it might reach into. The narrow surface is the prompt.
