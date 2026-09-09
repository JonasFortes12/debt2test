package io.github.jonasfortes12.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boots JPA, Flyway, and the persistence adapters.
 *
 * <p>Deliberately placed at the module's package root, so {@code @SpringBootApplication}'s
 * component scan and entity scan pick up {@code entity}, {@code repository}, and
 * {@code adapter} without explicit scan annotations, whose packages have moved between
 * Spring Boot versions. Without the component scan the adapters are never registered as
 * beans and {@code getBean} fails at bootstrap.
 *
 * <p>The CLI is not a Spring application, so this is loaded into a headless context only when
 * persistence is enabled. A future REST module can import the same class.
 */
@SpringBootApplication
public class PersistenceConfiguration {
}
