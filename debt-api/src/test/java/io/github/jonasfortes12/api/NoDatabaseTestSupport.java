package io.github.jonasfortes12.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.github.jonasfortes12.core.port.PipelineRunStore;
import io.github.jonasfortes12.core.port.ReportSink;
import io.github.jonasfortes12.persistence.access.AccessCodeStore;

/**
 * Boots the full API context without a database, so Docker-free tests (the Swagger completeness
 * test, the smoke test) stay in the default {@code mvn test} run. It skips the import of
 * {@code debt-persistence} via the {@code debt.api.test.skip-database} test seam, switches off the
 * database auto-configuration, and mocks the stores the database would have provided.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(properties = {
        "debt.api.test.skip-database=true",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@MockitoBean(types = { PipelineRunStore.class, ReportSink.class, AccessCodeStore.class })
public @interface NoDatabaseTestSupport {
}
