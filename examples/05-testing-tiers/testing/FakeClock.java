package com.example.booking.testing.time;

import com.example.booking.shared.time.Clock;

import java.time.Duration;
import java.time.Instant;

/**
 * Test-only {@link Clock} whose instant is driven explicitly. Inject this in
 * any test that touches a temporal rule (cancellation windows, reminder lead
 * times, expiry) so the behavior is deterministic instead of "depends on when
 * CI ran". It's the adapter that makes the {@code Clock} port pay off.
 *
 * <p>No mock framework — a fake is a real object with real behavior you can
 * advance. It reads clearly in the test and the compiler keeps it honest.
 */
public final class FakeClock implements Clock {

    private Instant current;

    private FakeClock(Instant start) {
        this.current = start;
    }

    public static FakeClock startingAt(Instant start) {
        return new FakeClock(start);
    }

    @Override
    public Instant instant() {
        return current;
    }

    public void advance(Duration by) {
        current = current.plus(by);
    }

    public void set(Instant to) {
        current = to;
    }
}
