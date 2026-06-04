package com.example.booking.bus;

import java.time.Instant;
import java.util.List;

/**
 * A narrow abstraction over whatever cloud message-bus SDK is in use. This is
 * the seam that keeps the vendor SDK out of everything above it: only the
 * adapter in this package implements it, and it speaks in plain records, not
 * SDK request/response types.
 *
 * <p>The real implementation wraps a managed event-bus client; a test or
 * local build can provide an in-memory one that just records what was
 * published. (Infrastructure specifics — bus names, regions, credentials —
 * are deliberately out of scope for this reference repo.)
 */
public interface MessageBusClient {

    /** Publish a batch; returns one {@link Outcome} per input entry, in order. */
    List<Outcome> publish(List<BusMessage> messages);

    record BusMessage(String source, String type, String payload, Instant time) {
    }

    /** Per-entry result — {@code ok} or a failure reason for retry/DLQ bookkeeping. */
    record Outcome(boolean ok, String errorOrNull) {
    }
}
