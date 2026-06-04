/**
 * The tenant bounded context.
 *
 * <p>The {@code @ApplicationModule} annotation declares this module's
 * <em>allowed dependencies</em>. A reference to any other bounded context
 * that isn't listed here fails the Modulith verification test on the next
 * build (example 04). This context depends only on the shared kernel
 * ({@code shared}), so a stray import of, say, the booking aggregate would
 * be caught mechanically — not in code review, if you're lucky.
 *
 * <p>Other modules consume this one as {@code "tenant :: contract"}: they
 * may import the {@link com.example.booking.tenant.contract} package and
 * nothing else.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "shared" }
)
package com.example.booking.tenant;
