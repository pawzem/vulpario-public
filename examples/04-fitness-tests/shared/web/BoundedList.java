package com.example.booking.shared.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method that returns a raw collection on purpose, because
 * the result set is bounded by a domain rule (e.g. "the seven days of a
 * week", "a tenant's at-most-handful of branches"). The
 * {@code PaginationVerificationTest} accepts a list-returning endpoint only if
 * it either takes a {@link org.springframework.data.domain.Pageable} or
 * carries this annotation with a non-blank {@code reason}.
 *
 * <p>The {@code reason} is the point: it forces whoever bypasses pagination to
 * write down <em>why</em> the list can't grow, where a reviewer (and the next
 * person) can see it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BoundedList {

    /** Why this list is bounded by domain rules and safe to return un-paginated. */
    String reason();
}
