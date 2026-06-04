# 07 · Frontend feature — the same discipline, across the wire

The frontend isn't a second architecture; it's the same one in TypeScript.
**One feature folder per backend bounded context.** A single `api.ts` per
feature is the only place that calls the HTTP client. Wire data is parsed into
branded domain types at that boundary. Server state lives in the query cache.
And the boundaries are linted, the way the backend's are tested.

## What to look at

```
shared/                                  the frontend kernel
├── auth/AuthAdapter.ts                  a port (mirror of backend example 03)
└── http/createApiClient.ts              the one HTTP client every feature shares
features/subscription/                   one feature = one backend BC
├── index.ts                             the feature's public surface (its "contract")
├── api.ts                               the ONLY caller of the HTTP client
├── types.ts                             branded ids + domain types
├── hooks/useSubscription.ts             TanStack Query — the only thing components use
├── fixtures/handlers.ts                 MSW handlers (HTTP test fixtures)
└── __tests__/api.test.ts               unit test of the parse boundary
eslint.config.boundaries.mjs             enforces feature-per-BC at lint time
```

## The rules, and how they map to the backend

| Frontend rule | Backend echo (examples) |
| --- | --- |
| One feature folder per backend BC | One module per bounded context (01) |
| `index.ts` is the only public surface | `contract` package is the only public surface (01) |
| `api.ts` is the only HTTP-client caller | the controller is the only transport layer (01) |
| Wire types parsed to branded domain types at `api.ts` | DTOs/value objects at the service boundary (01, 06) |
| Server state in TanStack Query, never duplicated | the service is the read model (01) |
| Ports for auth/HTTP | ports for email/bus/time (03) |
| `eslint-plugin-boundaries` fails the lint | `ApplicationModulesTest` fails the build (04) |

## Two boundaries worth dwelling on

**`api.ts` as the anti-corruption layer.** It owns the `*Wire` shapes — what
the server literally sends — and converts them into the feature's domain types:
branding ids (`PlanId(wire.id)`), narrowing unions, turning a `404` into a
domain `null`. Nothing past `api.ts` ever touches raw wire data. When the
backend response shape shifts, exactly one file changes.

**Branded ids.** `PlanId` and `SubscriptionId` are `string`s the compiler won't
let you interchange or fabricate — they're minted only in `api.ts` after a
format check. It's the TypeScript echo of the backend's `TenantId` / `PlanId`
value objects, and it kills the "I passed the subscription id where the plan id
goes" class of bug at compile time.

## Why this matters for AI-assisted work

`eslint-plugin-boundaries` does for the frontend what the Modulith test does
for the backend: it makes the boundary something a model gets corrected on
automatically. Ask for "a plans dropdown" and if the generated component
deep-imports `features/subscription/api.js`, the lint goes red — the model
sees the same error you would, and the architecture holds without a human
catching it in review.

> The real components, i18n, and a Playwright e2e tier are omitted here to keep
> the example focused on the boundaries. `@bookly/shared` stands in for the
> kernel package; provider/auth specifics are out of scope (see the top-level
> README's redaction note).
