package io.github.jonasfortes12.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.github.jonasfortes12.persistence.PersistenceConfiguration;

/**
 * Boots {@code debt-persistence} (DataSource, Flyway, JPA, the run store, the report sink and the
 * access-code store) inside this API's own Spring context.
 *
 * <p>{@code debt.api.test.skip-database} is a test seam only, so the Docker-free tests can boot the
 * API without a database; it is deliberately undocumented. Setting it outside tests does not
 * produce an unprotected API: without these stores the context fails to start.
 */
@Configuration
@ConditionalOnProperty(name = "debt.api.test.skip-database", havingValue = "false", matchIfMissing = true)
@Import(PersistenceConfiguration.class)
public class PersistenceImportConfig {
}
