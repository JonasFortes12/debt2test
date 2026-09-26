package io.github.jonasfortes12.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import io.github.cdimascio.dotenv.Dotenv;

class DotenvEnvironmentPostProcessorTest {

    private static Dotenv dotenvIn(Path directory) {
        return Dotenv.configure().directory(directory.toString()).ignoreIfMissing().load();
    }

    @Test
    void addsOnlyTheKnownKeysFromDotenv(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve(".env"), String.join("\n",
                "DEBT_API_PORT=9090",
                "DEBT_PERSISTENCE_ENABLED=true",
                "SOME_UNRELATED_VARIABLE=should-not-leak-through",
                ""), StandardCharsets.UTF_8);
        MockEnvironment environment = new MockEnvironment();

        DotenvEnvironmentPostProcessor.apply(environment, dotenvIn(directory));

        assertEquals("9090", environment.getProperty("DEBT_API_PORT"));
        assertEquals("true", environment.getProperty("DEBT_PERSISTENCE_ENABLED"));
        assertNull(environment.getProperty("SOME_UNRELATED_VARIABLE"),
                "only the fixed, known key list may be added -- never an arbitrary .env entry");
    }

    @Test
    void realPropertiesStillWinOverDotenvValues(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve(".env"), "DEBT_API_PORT=9090\n", StandardCharsets.UTF_8);
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DEBT_API_PORT", "8080");

        DotenvEnvironmentPostProcessor.apply(environment, dotenvIn(directory));

        assertEquals("8080", environment.getProperty("DEBT_API_PORT"),
                "a real environment/test property must never be shadowed by .env");
    }

    @Test
    void addsNoPropertySourceWhenNoEnvFileExists(@TempDir Path emptyDirectory) {
        MockEnvironment environment = new MockEnvironment();
        int before = environment.getPropertySources().size();

        DotenvEnvironmentPostProcessor.apply(environment, dotenvIn(emptyDirectory));

        assertEquals(before, environment.getPropertySources().size());
    }
}
