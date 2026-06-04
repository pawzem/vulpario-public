package com.example.booking.testing.outbox;

import com.example.booking.shared.outbox.OutboxEvent;
import com.example.booking.shared.outbox.OutboxWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Capturing {@link OutboxWriter} double — records every {@code append} instead
 * of hitting the database. Lets a unit or story test assert "this command
 * wrote exactly this integration event" without a Testcontainers Postgres
 * spinning up. Shared across every context's tests.
 */
public final class RecordingOutboxWriter implements OutboxWriter {

    public final List<OutboxEvent> events = new ArrayList<>();

    @Override
    public void append(OutboxEvent event) {
        events.add(event);
    }

    public void clear() {
        events.clear();
    }
}
