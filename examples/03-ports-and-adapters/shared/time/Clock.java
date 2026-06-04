package com.example.booking.shared.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * The domain's time <strong>port</strong>. Inject this instead of calling
 * {@code Instant.now()} directly, so a {@code FakeClock} (example 05) can
 * drive time in tests and temporal rules become fully deterministic. A tiny
 * port, but it removes an entire category of flaky test.
 */
public interface Clock {

    Instant instant();

    default LocalDate today() {
        return instant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    default LocalDate today(ZoneId zone) {
        return instant().atZone(zone).toLocalDate();
    }
}
