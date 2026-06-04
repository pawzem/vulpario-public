package com.example.booking.shared.time;

import java.time.Instant;

/** The production {@link Clock} adapter — the real wall clock. */
public final class SystemClock implements Clock {

    private static final SystemClock INSTANCE = new SystemClock();

    public static SystemClock instance() {
        return INSTANCE;
    }

    private SystemClock() {
    }

    @Override
    public Instant instant() {
        return Instant.now();
    }
}
