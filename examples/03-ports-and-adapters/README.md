# 03 · Ports & adapters — the edges of the system

Anything that talks to the outside world — email, SMS, the message bus,
object storage, payments — sits behind a **port** (a plain interface in the
shared kernel). Domain code depends only on the port; the vendor SDK lives in
a swappable **adapter** at the edge. Two house rules make this pleasant
rather than ceremonial.

## What to look at

```
shared/email/EmailSender.java         the port — domain language, no vendor types
shared/email/LoggingEmailSender.java  the safe fallback (bean-present on every boot)
email/SmtpEmailSender.java            a real adapter, property-gated, @Primary when on
shared/time/Clock.java                another port — time
shared/time/SystemClock.java          its production adapter
bus/MessageBusClient.java             a narrow seam over the cloud bus SDK
bus/MessageBusPublisher.java          adapter implementing OutboxPublisher (from example 02)
```

## Rule 1 — there's always a safe fallback, wired on every boot

`LoggingEmailSender` implements the port by logging and dropping the message.
It's present in every configuration. Consequences:

- The port is **exercised at context load** even when no real transport is
  configured — you find wiring mistakes at startup, not at 2am.
- **Local dev never crashes** for lack of a cloud credential. You can run the
  whole app and watch emails scroll past in the log.
- Turning on the real thing is a property flip: `SmtpEmailSender` is gated on
  `booking.email.transport=smtp` and registers `@Primary`, so it transparently
  overrides the fallback.

## Rule 2 — no vendor type crosses the port

`EmailSender.send(EmailMessage)` speaks recipient/subject/attachment.
`SmtpEmailSender` is the *only* place that knows about MIME and
`JavaMailSender`. `MessageBusPublisher` is the *only* place that knows a
message bus exists; everything above it sees the `OutboxPublisher` port from
example 02. Swap SMTP for a cloud email API, or one bus for another, and you
write a new adapter and flip a property — no domain code changes.

`MessageBusClient` shows the same idea one level deeper: even the adapter
doesn't hard-code the SDK's request/response types; it depends on a narrow
in-house seam that's trivial to fake in a test.

## Why this matters for AI-assisted work

Ports give a model a tiny, honest target. "Add SMS reminders" becomes
"implement this three-method `SmsSender` interface" — not "go wire an SDK
into the reminder service and hope you didn't leak its types into the domain."
The fallback-on-every-boot rule means a half-finished adapter still leaves the
app bootable and the tests green, so you can land it incrementally.

> Cloud/account specifics (bus names, regions, credentials, the actual
> provider) are intentionally omitted from this reference repo — see the
> top-level README's redaction note. The point here is the *shape*, which is
> provider-neutral by design.
