# Bookly — a reference modular monolith

A curated, anonymized set of code examples from a production multi-tenant
booking/scheduling SaaS, extracted for a conference talk on **what software
architecture looks like in the age of AI-assisted ("vibe") coding**.

The thesis of the talk — and of this repository — is simple:

> AI lets you produce code at a rate that used to be impossible. The
> bottleneck moves from *typing* to *keeping the system coherent*. The
> answer isn't less AI; it's **stronger architecture and executable
> guardrails** that make the disciplined path the path of least
> resistance — for a human and for a model.

So these examples lean on a few old, boring, load-bearing ideas — a
**modular monolith** with enforced boundaries, the **transactional
outbox**, **ports & adapters**, **rich aggregates**, and a test pyramid —
and on a set of **architecture fitness functions** that fail the build the
moment a boundary is crossed, a list endpoint forgets to paginate, or an
aggregate grows a JavaBean setter. Those checks are what let you point a
model at the codebase and trust the result.

> [!NOTE]
> **These are illustrative snippets, not a buildable project.** They were
> lifted from a private codebase, renamed to a neutral `com.example.booking`
> package, stripped of all infrastructure, account, and commercial detail,
> and reorganized by pattern. They compile in spirit, not on your machine —
> read them, don't `./gradlew build` them. See
> [Provenance & redaction](#provenance--redaction).

## The stack these patterns ride on

| Layer | Choice |
| --- | --- |
| Language / runtime | Java 25 |
| Framework | Spring Boot 4, **Spring Modulith** (module boundary verification) |
| Persistence | PostgreSQL via **Spring Data JDBC** (no JPA), optimistic locking, JSONB aggregate state |
| Integration | **Transactional outbox** → a message bus, CloudEvents-shaped payloads |
| Testing | JUnit 5, ArchUnit, Testcontainers — **no Mockito** (hand-written stubs/fakes) |
| Frontend | React + TypeScript (`strict`), TanStack Query, MSW, `eslint-plugin-boundaries` |

## What's here

Each folder under [`examples/`](./examples/) is one pattern, with its own
`README.md` explaining the idea and what to look at.

| # | Example | The idea |
| --- | --- | --- |
| 01 | [`service-facade`](./examples/01-service-facade/) | One interface per bounded context is the *only* public surface. Aggregates, repos, controllers stay package-private. |
| 02 | [`outbox`](./examples/02-outbox/) | Emit integration events by writing a row in the **same transaction** as the state change; a dispatcher drains it with retry + dead-letter. |
| 03 | [`ports-and-adapters`](./examples/03-ports-and-adapters/) | Domain code depends on a port (`EmailSender`, `MessageBusPublisher`); transports are swappable adapters with a safe logging fallback. |
| 04 | [`fitness-tests`](./examples/04-fitness-tests/) | The guardrails. Module boundaries, "no setters on aggregates", and "every list paginates" — enforced as tests that fail the build. |
| 05 | [`testing-tiers`](./examples/05-testing-tiers/) | Unit → story → integration, no Mockito. Fast feedback at the bottom, real Postgres at the top. |
| 06 | [`cross-bc-collaboration`](./examples/06-cross-bc-collaboration/) | One context calls another **only** through its published contract, declared in the module dependency whitelist. |
| 07 | [`frontend-feature`](./examples/07-frontend-feature/) | The frontend mirrors the backend: one feature folder per BC, a single `api.ts` boundary, branded IDs, boundaries enforced by lint. |

A deeper written narrative — the "why" behind the layout — lives in
[`docs/architecture.md`](./docs/architecture.md).

## How to read this for the talk

1. Start with [`docs/architecture.md`](./docs/architecture.md) for the big picture.
2. [`01-service-facade`](./examples/01-service-facade/) shows the unit of modularity.
3. [`04-fitness-tests`](./examples/04-fitness-tests/) is the punchline: this is *how you keep AI honest*.
4. The rest fill in reliability (`02`), decoupling (`03`, `06`), and the test/UI story (`05`, `07`).

## Provenance & redaction

This repository is a teaching artifact. To protect commercial IP while
keeping the architecture honest, the following were deliberately changed or
removed:

- **Renamed** — the real product name, company, and Java package became
  `Bookly` / `com.example.booking`. The domain vocabulary (tenant, plan,
  subscription, booking, slot) is generic and kept as-is.
- **Removed** — all cloud/account/infrastructure detail (IaC templates,
  ARNs, regions, identifiers), third-party integrations, business-specific
  algorithms, and internal roadmap references.
- **Simplified** — services and aggregates were trimmed to the smallest
  shape that still demonstrates the pattern cleanly. Production versions
  carry more methods, validation, and edge-case handling.

Nothing here is a secret or a credential. If you're giving a similar talk,
take the patterns — they're industry-standard and none of them are mine to
gatekeep.

## License

This repository is a **reference and teaching artifact, not a software
dependency** — published so people can read and learn from it, not reuse it.
It's licensed under
**[CC BY-NC-ND 4.0](./LICENSE)** (Creative Commons
Attribution–NonCommercial–NoDerivatives): you may share it with attribution,
but **no commercial use** and **no redistribution of modified versions**.
It is provided **as-is, without warranty**.

The underlying *patterns* — modular monolith, transactional outbox, ports &
adapters, architecture fitness functions — are industry-standard ideas, are
not subject to copyright, and remain free for anyone to use, including
commercially. The license covers only this particular collection of code and
prose.
