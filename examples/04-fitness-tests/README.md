# 04 · Architecture fitness functions — the guardrails

This is the punchline of the talk. Every convention the other examples
describe is also **enforced as an ordinary test that fails the build.** A
fitness function turns "we agreed to do it this way" into "the CI is red until
you do it this way" — which is the only kind of agreement that survives
contact with a deadline, a new hire, or a model generating half the diff.

## What to look at

```
modulith/ApplicationModulesTest.java          module boundaries hold
modulith/NoPublicSettersInAggregatesTest.java aggregates have no JavaBean setters
modulith/PaginationVerificationTest.java       every list endpoint paginates
shared/web/BoundedList.java                    the escape hatch the pagination rule allows
scripts/check-ubiquitous-language.mjs          one concept, one word (backend + frontend)
```

## The four checks

| Check | Fails the build when… | Replaces |
| --- | --- | --- |
| **Module boundaries** | a context references another outside its declared `allowedDependencies` | an architect re-reading every import |
| **No setters on aggregates** | a `@Table` class has a `public void setX(arg)` | a reviewer remembering the "rich aggregate" rule |
| **Pagination** | a controller returns a raw `List`/`Set` without a `Pageable` or `@BoundedList` | a reviewer noticing the unbounded query |
| **Ubiquitous language** | a banned synonym (`Appointment`, `Customer`, …) appears in source | a reviewer policing vocabulary drift |

Each is a few dozen lines. Together they cover the rules that otherwise eat
the most review attention and degrade the most quietly over time.

## Why this is the AI-era thesis in one folder

When a model writes a plausible service in seconds, your reviewers can't read
every line as carefully as they used to — there's too much, too fast. The
checks that *can* keep up are the ones that don't depend on attention:

- They run on every push, on 100% of the diff, without getting tired.
- They give the model a **red bar it can act on** — the exact same signal a
  human gets — instead of a style note buried in a PR thread.
- They encode the judgment of your most senior engineer once, so it applies
  even when that engineer isn't in the review.

The boundaries from example 01 aren't a suggestion the model is trusted to
honor; `ApplicationModulesTest` makes them load-bearing. That's the difference
between "we have an architecture" and "our architecture is enforceable."

## Note on these snippets

`ApplicationModulesTest` references `com.example.booking.BooklyApplication`
(the Spring Boot entry point — not included here). The ArchUnit and
classpath-scanning checks scan the `com.example.booking` base package. In the
real repo all four run inside `./gradlew build`; the JS gate runs in the CI's
frontend job. They're shown here as-is so you can lift the technique.
