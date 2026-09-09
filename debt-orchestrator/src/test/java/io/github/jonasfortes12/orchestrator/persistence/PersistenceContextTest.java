package io.github.jonasfortes12.orchestrator.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.jonasfortes12.core.port.PipelineRunStore;

class PersistenceContextTest {

    @Test
    void persistenceIsDisabledWhenTheFlagIsAbsent() {
        Optional<PersistenceContext> context = PersistenceContext.openIfEnabled(name -> null);

        assertTrue(context.isEmpty(),
                "the CLI must run with no database unless persistence is explicitly enabled");
    }

    @Test
    void persistenceIsDisabledWhenTheFlagIsFalse() {
        Optional<PersistenceContext> context = PersistenceContext.openIfEnabled(
                name -> PersistenceContext.ENABLED_VARIABLE.equals(name) ? "false" : null);

        assertTrue(context.isEmpty());
    }

    @Test
    void anUnavailableDatabaseDegradesInsteadOfFailingTheProcess() {
        // Only the enabled flag is read through this function; the datasource comes from the
        // Spring environment, which is unconfigured here, so the context cannot start.
        Optional<PersistenceContext> context = PersistenceContext.openIfEnabled(
                name -> PersistenceContext.ENABLED_VARIABLE.equals(name) ? "true" : null);

        assertTrue(context.isPresent(), "an outage must not abort the run");
        assertFalse(context.get().isAvailable());

        // The store throws, which the application service turns into a recoverable
        // PERSISTENCE_WRITE_FAILED error rather than losing the run.
        PipelineRunStore store = context.get().runStore();
        assertThrows(IllegalStateException.class, () -> store.runStarted("exec-1", null));

        context.get().close();
    }
}
