package io.github.jonasfortes12.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Boots JPA, Flyway, and the persistence adapters.
 *
 * <p>Deliberately placed at the module's package root: Boot's default scanning then picks up
 * {@code entity}, {@code repository}, and {@code adapter} without explicit scan annotations,
 * whose packages have moved between Spring Boot versions.
 *
 * <p>The CLI is not a Spring application, so this is loaded into a headless context only when
 * persistence is enabled. A future REST module can import the same class.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class PersistenceConfiguration {
}
