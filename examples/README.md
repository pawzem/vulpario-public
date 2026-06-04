# Examples

Each folder is one pattern, with its own `README.md`. They're meant to be
read in order, but each stands on its own.

> These are anonymized, simplified snippets — read them, don't build them.
> Package is `com.example.booking`; infrastructure and commercial logic are
> removed. See the repo [README](../README.md) and
> [architecture notes](../docs/architecture.md).

| # | Folder | Pattern |
| --- | --- | --- |
| 01 | [`service-facade`](./01-service-facade/) | One public `XxxService` interface per bounded context; everything else package-private. |
| 02 | [`outbox`](./02-outbox/) | Transactional outbox — events written in the same transaction as state, drained with retry + dead-letter. |
| 03 | [`ports-and-adapters`](./03-ports-and-adapters/) | Ports in the kernel, swappable adapters at the edge, a safe fallback on every boot. |
| 04 | [`fitness-tests`](./04-fitness-tests/) | Architecture rules encoded as build-failing tests — the AI-era guardrails. |
| 05 | [`testing-tiers`](./05-testing-tiers/) | Unit → story → integration, with hand-written test doubles (no Mockito). |
| 06 | [`cross-bc-collaboration`](./06-cross-bc-collaboration/) | One context depends on another only through its published contract. |
| 07 | [`frontend-feature`](./07-frontend-feature/) | The frontend's feature-per-BC mirror of the same discipline. |

## A note on Java version

The snippets use Java 21+ features freely — records, sealed types, pattern
matching in `switch`, text blocks. They're written against a recent JDK and
Spring Boot 4 / Spring Framework 7. Nothing here depends on a preview
feature.
