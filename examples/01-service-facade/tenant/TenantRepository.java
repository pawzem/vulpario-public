package com.example.booking.tenant;

import com.example.booking.shared.identity.TenantId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Package-private Spring Data JDBC repository. Note what's <em>not</em> here:
 * no {@code @Repository}-exposed-as-public, no leaking of {@code Tenant} past
 * the package. The interface extends {@link CrudRepository} for the basics
 * and adds two explicit queries.
 *
 * <p>{@code findAllPaged} takes a {@link Pageable} and returns a bounded
 * slice — list endpoints never load an unbounded table (enforced by the
 * pagination fitness test in example 04).
 */
interface TenantRepository extends CrudRepository<Tenant, TenantId> {

    @Query("SELECT * FROM tenant WHERE slug = :slug")
    Optional<Tenant> findBySlug(@Param("slug") String slug);

    @Query("SELECT * FROM tenant ORDER BY lower(name) ASC LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    List<Tenant> findAllPaged(Pageable pageable);
}
