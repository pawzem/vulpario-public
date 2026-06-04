# Architecture notes

Background reading for the [examples](../examples/). This is the "why"
behind the code; each example folder has the "what to look at".

> Reminder: this is an anonymized reference extracted from a production
> system. Names, infrastructure, and commercial logic are removed or
> simplified. See the repo [README](../README.md#provenance--redaction).

## The thesis: AI raises the value of architecture, it doesn't lower it

When a model can write a plausible service, repository, controller, and test
in seconds, the scarce resource stops being *code* and becomes *coherence* —
does the new code respect the boundaries, the invariants, the conventions
that keep the system changeable a year from now?

Two things make that coherence cheap to maintain:

1. **A shape that's hard to violate by accident.** Small modules with one
   public surface each. If the wrong thing isn't reachable, neither a human
   nor a model can casually couple to it.
2. **Executable guardrails.** Rules that used to live in a wiki or a senior
   engineer's head, rewritten as tests that fail the build. A model can't
   read your intentions, but it gets a red bar — and so do you, in code
   review, before the design erodes.

Everything below serves one of those two ends.

## A modular monolith, not microservices

The system is a single deployable split into **bounded contexts** (BCs) —
tenant, plan, subscription, and others — each a module with enforced
boundaries, but sharing a process, a database, and a transaction manager.

Why a monolith and not services:

- **One transaction across a state change and its event.** The
  [transactional outbox](../examples/02-outbox/) is trivial when the
  aggregate write and the outbox row are in the same DB transaction. Across
  services you'd need sagas or two-phase nonsense for the same guarantee.
- **Refactoring across a boundary is a compile + a test run**, not a
  cross-repo migration with version skew.
- **The boundaries are still real** — they're verified on every build (see
  [fitness tests](../examples/04-fitness-tests/)). So when a context
  genuinely needs to become a service later, the seam already exists.

The module framework (Spring Modulith) lets each BC declare exactly which
other BCs it may depend on, and fails the build on any undeclared reference.

## Each bounded context is one facade

This is the single most important convention. **Every BC exposes exactly one
public interface — `XxxService` — and nothing else.** Controllers, other
BCs, and tests all go through it.

```
platform.tenant/
├── contract/                ← the ONLY public package (a "named interface")
│   ├── TenantService.java       interface: commands + queries
│   ├── TenantServiceStub.java   in-memory impl for other modules' tests
│   ├── TenantDto.java           returned by the service
│   ├── TenantRegistered.java    integration event
│   └── TenantSlugAlreadyTakenException.java
├── Tenant.java              ← aggregate (package-private)
├── TenantServiceImpl.java   ← production impl (package-private)
├── TenantController.java    ← REST transport (package-private)
├── TenantRepository.java    ← persistence (package-private)
└── package-info.java        ← declares allowed dependencies
```

Consequences:

- The **service is the read model.** Queries (`findById`, `findAll`,
  `exists`) live right on the interface. There's no separate "read model"
  or "view" abstraction — the facade is it.
- **Aggregates, repos, controllers, configs, and policies are
  package-private.** They start that way and get promoted to `public` only
  when a test outside the package genuinely needs them. Default-closed.
- A consumer can hold a `TenantService` and has *no way* to reach a
  `Tenant` aggregate or a repository — the type isn't visible.

See [`01-service-facade`](../examples/01-service-facade/).

## Controllers are transport only

A REST controller binds the request, calls **one** service method, maps the
returned DTO to a wire record, and lets exceptions propagate to a
per-BC handler. It does not orchestrate multiple services, enrich responses
from sibling BCs, or hold business logic. If a response needs a field from
another BC, the *service* injects that BC's facade and returns a fully formed
DTO — the controller stays a one-liner per route.

## Rich aggregates, no setters

Aggregates expose **intention-revealing commands** (`register`, `rename`,
`suspend`) that enforce invariants and emit domain events — never JavaBean
`setX` setters that let any caller put the object into any state. This is
enforced mechanically: see the "no public setters on aggregates" check in
[`04-fitness-tests`](../examples/04-fitness-tests/).

State lives in the database as columns plus JSONB for the structured bits;
persistence is Spring Data JDBC (no JPA, no lazy-loading surprises, no
detached-entity foot-guns). Optimistic locking via a `version` column.

## Events: the transactional outbox

Integration events flow through an outbox:

1. A command mutates an aggregate, which buffers a domain event.
2. The repository's `save()` writes the aggregate row **and** an
   `outbox_event` row in the **same transaction**. If the transaction rolls
   back, the event vanishes with the state change. No event ever describes a
   state that didn't commit; no state ever commits without its event.
3. A dispatcher polls the outbox, publishes each row to the message bus,
   and marks it published — with exponential-backoff retries and a
   dead-letter state for poison messages. At-least-once delivery; consumers
   dedupe on the event id.

In-process consumers (policies in sibling BCs) are reached by re-publishing
the same event on the application event bus after the outbox row is written,
so a saga can run in-process today and over the bus tomorrow without changing
the policy code.

See [`02-outbox`](../examples/02-outbox/).

## Ports & adapters at the edges

Anything that talks to the outside world — email, SMS, the message bus,
object storage, the billing provider — is a **port** (a plain interface in
the shared kernel) with swappable **adapters**. Domain code depends only on
the port.

Two rules make this pleasant:

- **There is always a safe fallback adapter** wired on every boot (e.g. a
  `LoggingEmailSender` that logs and drops). The port is exercised at context
  load even when no real transport is configured, and local dev never
  crashes for lack of a cloud credential.
- **No vendor types leak across the port.** The interface speaks domain
  language; the SDK lives behind the adapter.

See [`03-ports-and-adapters`](../examples/03-ports-and-adapters/).

## Cross-context collaboration

A BC's service takes other BCs' **services** as constructor dependencies —
never their repositories or aggregates. In production Spring injects the real
impls; in tests you pass the `XxxServiceStub` from the other BC's `contract/`
package. The dependency is declared in the module's `package-info.java` and
verified on every build.

ID value objects (`TenantId`, `PlanId`, `SubscriptionId`, …) live in the
shared kernel, not in any one BC's contract — so an event that carries an id
doesn't drag the issuing BC into the consumer's dependency graph. That's how
you keep the module graph acyclic.

See [`06-cross-bc-collaboration`](../examples/06-cross-bc-collaboration/).

## Testing: a real pyramid, no Mockito

Three tiers, fast to slow:

1. **Unit** — one class, no Spring. Cross-BC dependencies are hand-written
   stubs from the other module's `contract/`. Milliseconds.
2. **Story** — wire the real classes of the BC under test with stubs for
   external BCs. Exercises a use case end-to-end inside one context.
3. **Integration** — Testcontainers Postgres, real JDBC repositories.
   Verifies the SQL, the JSONB mapping, the optimistic lock.

**No mocking framework.** Test doubles are explicit, hand-written, and
reusable (`FakeClock`, `StubEmailSender`, `RecordingOutboxWriter`). They're
readable, they don't drift from the interface (the compiler keeps them
honest), and they double as living documentation of the port.

See [`05-testing-tiers`](../examples/05-testing-tiers/).

## Architecture fitness functions

The guardrails. Each is an ordinary test in the build, so a violation fails
CI like any other red test:

- **Module boundaries** — `ApplicationModules.of(App.class).verify()` rejects
  any cross-BC reference not declared in the target module's allowed
  dependencies.
- **No setters on aggregates** — an ArchUnit rule that fails on any
  `public void setX(arg)` on a persistence aggregate.
- **Every list paginates** — a reflective scan that fails any controller
  method returning a raw `List`/`Set`/`Collection` without a `Pageable`
  (or an explicit, justified `@BoundedList`).
- **Ubiquitous-language gate** — a script that fails the build if banned
  synonyms (off-glossary terms) appear in source, keeping one word per
  concept across backend and frontend.

These are the rules you'd otherwise repeat in every code review. Encoded
once, they hold the line automatically — which is exactly what you want when
a chunk of the code arriving in review was written by a model.

See [`04-fitness-tests`](../examples/04-fitness-tests/).

## The frontend mirrors the backend

The same discipline crosses the wire. One **feature folder per backend BC**;
a single `api.ts` per feature is the only place that calls the HTTP client;
wire types are parsed into branded domain types at that boundary; server
state lives in the query cache, never duplicated into component state. And —
like the backend — the boundaries are linted: a feature can only be reached
through its `index.ts`, enforced by `eslint-plugin-boundaries`.

See [`07-frontend-feature`](../examples/07-frontend-feature/).
