package com.example.booking.modulith;

import com.example.booking.BooklyApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * The boundary guard. One line of assertion enforces the entire module graph:
 * {@code ApplicationModules.of(...).verify()} walks every bounded context and
 * fails if any of them references another context outside its declared
 * {@code @ApplicationModule(allowedDependencies = ...)} list (see the
 * {@code package-info.java} files in example 01 and 06).
 *
 * <p>Contract sub-packages are exposed via {@code @NamedInterface("contract")}
 * and consumed as {@code "<bc> :: contract"} in the whitelists — so a context
 * may import another's published contract, but not its aggregate, repository,
 * or impl. The moment someone (a teammate, or a model) adds an import that
 * crosses a boundary the wrong way, {@code ./gradlew build} goes red on this
 * test, with a message naming the offending reference.
 *
 * <p>This is the cheapest architecture review you'll ever run: it costs one
 * test and it never gets tired or distracted.
 */
class ApplicationModulesTest {

    @Test
    void modulith_boundaries_are_respected() {
        ApplicationModules.of(BooklyApplication.class).verify();
    }
}
