/**
 * The tenant context's <em>only</em> public package. The
 * {@code @NamedInterface} annotation marks it as the published surface other
 * modules may import: a sibling context declares a dependency on
 * {@code "tenant :: contract"}, never on {@code com.example.booking.tenant}
 * itself. Everything outside this package — the aggregate, the repository,
 * the controller, the service impl — is package-private and unreachable.
 */
@org.springframework.modulith.NamedInterface("contract")
package com.example.booking.tenant.contract;
