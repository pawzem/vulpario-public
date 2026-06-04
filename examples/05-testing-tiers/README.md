# 05 · Three test tiers, and no Mockito

A real pyramid: lots of fast tests at the bottom, a few slow ones at the top.
Each tier answers a different question, and the test doubles are **hand-written
fakes, not mocks**.

## What to look at

```
unit/TenantTest.java                     tier 1 — the aggregate, pure, no Spring
testing/FakeClock.java                   reusable doubles (the no-Mockito convention)
testing/StubEmailSender.java
testing/RecordingOutboxWriter.java
story/TenantServiceTest.java             tier 2 — real service + in-memory deps
story/InMemoryTenantRepository.java
integration/JdbcTenantRepositoryTest.java tier 3 — real repo + Testcontainers Postgres
```

## The three tiers

| Tier | Under test | Collaborators | Speed | Answers |
| --- | --- | --- | --- | --- |
| **Unit** | one class | none / pure | ms | "do the invariants and events hold?" |
| **Story** | the real service | in-memory / fakes | ms–tens of ms | "does the use case behave end-to-end in this context?" |
| **Integration** | the real repository | Testcontainers Postgres | seconds | "is the SQL, mapping, and locking actually correct?" |

You write *many* unit and story tests and *few* integration tests. The
integration tier is precious — it's the only place that proves the database
half — so it's reserved for what only the database can tell you (the
optimistic-lock test is the canonical example).

## Why no Mockito

The doubles here — `FakeClock`, `StubEmailSender`, `RecordingOutboxWriter`,
`InMemoryTenantRepository` — are real implementations of the ports/interfaces.
That buys three things a mock doesn't:

- **The compiler keeps them honest.** Change the `EmailSender` interface and
  every fake that implements it fails to compile — you can't forget one. A
  mock's `when(...).thenReturn(...)` silently rots into a lie.
- **They read as behavior, not as a script.** `clock.advance(Duration.ofDays(2))`
  says what's happening; `when(clock.instant()).thenReturn(...)` says how the
  mock was rigged.
- **They're reusable.** One `StubEmailSender` serves every context's tests, so
  the double itself becomes living documentation of the port.

## The convention that makes story tests possible

Each context ships a `XxxServiceStub` in its `contract/` package (example 01)
for *other* contexts to use, and keeps an `InMemoryXxxRepository` under
`src/test` in its *own* package for its *own* story tests. So a story test
wires the real service of the context under test against in-memory versions of
that context's internals — and stubs of any sibling context's service. Real
where it matters, fake where it doesn't.

> These snippets reference `JdbcTestSupport` and the Liquibase-managed schema,
> which aren't included — see the redaction note in the top-level README. The
> shapes are faithful; the plumbing is elided.
