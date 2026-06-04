# 02 · Transactional outbox — events that can't lie

An integration event must describe something that actually happened. The
outbox pattern guarantees it: the event row is written in the **same database
transaction** as the state change. They commit together or roll back
together. No event ever announces a state that didn't persist; no state ever
persists without its event.

## The flow

```
   COMMAND (one transaction)                 BACKGROUND (separate, polled)
   ────────────────────────────             ──────────────────────────────
   service.registerTenant(...)
     └─ tenant.register(...)        emits    OutboxDispatcher.dispatchOnce()
         └─ save():                            ├─ poll N rows FOR UPDATE SKIP LOCKED
              ├─ INSERT tenant ───┐            ├─ publisher.publish(batch)
              └─ INSERT outbox ───┴─ COMMIT    └─ mark published / retry / dead-letter
                                                       │
   (roll back here → BOTH disappear)                   ▼
                                                   message bus → other services
```

## What to look at

```
shared/domain/AggregateRoot.java     emit()/pullPendingEvents() — the producer side
shared/outbox/
├── OutboxEvent.java                  the row: a CloudEvents-shaped payload + routing columns
├── OutboxWriter.java                 the one rule: write in the caller's transaction
├── JdbcOutboxWriter.java             the INSERT that joins the open transaction
└── OutboxPublisher.java              port to the bus (adapter in example 03)
outbox/OutboxDispatcher.java          poll → publish → mark, with backoff + dead-letter
tenant/
├── TenantOutboxEnvelope.java         wraps an event with its outbox-row identity
└── TenantOutboxListener.java         writes the outbox row AND re-publishes in-process
```

## The two halves

**Write side** is boring on purpose. `JdbcOutboxWriter.append` is a plain
INSERT with no transaction of its own — it joins whatever transaction the
`@Transactional` service opened. That's the entire trick. An integration test
asserts the rollback-parity property directly: throw after the aggregate
save, and the outbox row is gone too.

**Read side** is the `OutboxDispatcher`: poll a batch with
`FOR UPDATE SKIP LOCKED` (so it scales to multiple instances without
double-sending), publish, then mark each row published / retried / dead-
lettered. Failures get exponential backoff; poison messages land in a
dead-letter state for a human, never vanish. Delivery is **at-least-once** —
consumers dedupe on `event_id`.

## Two emission styles, one rule

- A hand-persisted aggregate extends `AggregateRoot` and the repository
  drains `pullPendingEvents()` during `save()`.
- A Spring Data JDBC aggregate (`Tenant`, example 01) returns its events from
  a `@DomainEvents` method and Spring publishes them after `save()`.

Either way the aggregate never touches the outbox. In production the
`@DomainEvents` method wraps each event so the listener knows its row
identity:

```java
@DomainEvents
Collection<Object> domainEvents() {
    return pendingEvents.stream()
        .map(e -> TenantOutboxEnvelope.forTenant(version, id, e))
        .toList();
}
```

(Example 01's `Tenant` returned the raw events instead, to keep the facade
example uncluttered. `TenantOutboxListener` here shows the envelope-aware
version.)

## The in-process re-publish

`TenantOutboxListener` does two things with each event: writes the durable
outbox row **and** re-publishes the raw contract event on Spring's
application bus. That second hop lets a sibling context's policy react
immediately via `@TransactionalEventListener(AFTER_COMMIT)` — the same policy
code works unchanged whether the event arrives in-process now or over the bus
later. It's the seam that keeps "modular monolith today, services tomorrow"
honest.
