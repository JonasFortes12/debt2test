package io.github.jonasfortes12.api.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Adds debt-api's own {@code .env}-sourced keys as the lowest-priority property source, run early
 * enough in Spring Boot's startup to affect placeholders in {@code application.properties} (e.g.
 * {@code server.port=${DEBT_API_PORT:8080}}) -- unlike a value read inside a {@code @Bean} method,
 * which runs too late to influence those. A real OS environment variable, a JVM system property,
 * or a test's {@code @TestPropertySource} always wins, since this is added last.
 *
 * <p>Looks up each key individually via {@link Dotenv#get(String)} against a fixed, known list,
 * rather than {@link Dotenv#entries()}: {@code entries()} silently merges in the entire OS
 * environment for any key missing from the file, which would leak every unrelated environment
 * variable into this property source.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final List<String> KEYS = List.of(
            "DEBT_API_PORT",
            "DEBT_API_ALLOWED_ORIGIN",
            "DEBT_PERSISTENCE_ENABLED",
            "DEBT_DB_URL",
            "DEBT_DB_USERNAME",
            "DEBT_DB_PASSWORD",
            "DEBT_DB_POOL_SIZE");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        apply(environment, Dotenv.configure().ignoreIfMissing().load());
    }

    static void apply(ConfigurableEnvironment environment, Dotenv dotenv) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : KEYS) {
            String value = dotenv.get(key);
            if (value != null) {
                values.put(key, value);
            }
        }
        if (!values.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource("dotenv", values));
        }
    }
}
