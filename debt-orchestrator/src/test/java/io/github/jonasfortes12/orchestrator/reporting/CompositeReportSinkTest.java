package io.github.jonasfortes12.orchestrator.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.ReportArtifact;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.model.RunStatus;
import io.github.jonasfortes12.core.port.ReportSink;

class CompositeReportSinkTest {

    private static final ReportArtifact PRIMARY_ARTIFACT =
            new ReportArtifact(List.of(Path.of("output/debt-report.json")));

    private static PipelineResult result() {
        return new PipelineResult("run-1", RunStatus.COMPLETED, null, List.of(), List.of(), null);
    }

    private static ReportOptions options() {
        return new ReportOptions(Path.of("output"));
    }

    @Test
    void returnsThePrimaryArtifactAndInvokesEverySecondary() {
        AtomicBoolean secondaryCalled = new AtomicBoolean(false);
        ReportSink primary = (r, o) -> PRIMARY_ARTIFACT;
        ReportSink secondary = (r, o) -> {
            secondaryCalled.set(true);
            return null;
        };

        ReportArtifact artifact = new CompositeReportSink(primary, List.of(secondary))
                .write(result(), options());

        assertEquals(PRIMARY_ARTIFACT, artifact);
        assertTrue(secondaryCalled.get());
    }

    @Test
    void aFailingSecondaryDoesNotBreakTheReport() {
        ReportSink primary = (r, o) -> PRIMARY_ARTIFACT;
        ReportSink exploding = (r, o) -> {
            throw new IllegalStateException("database is down");
        };

        ReportArtifact artifact = new CompositeReportSink(primary, List.of(exploding))
                .write(result(), options());

        assertEquals(PRIMARY_ARTIFACT, artifact);
    }

    @Test
    void aFailingPrimaryStillPropagates() {
        ReportSink primary = (r, o) -> {
            throw new IllegalStateException("disk full");
        };

        CompositeReportSink sink = new CompositeReportSink(primary, List.of());

        assertThrows(IllegalStateException.class, () -> sink.write(result(), options()));
    }

    @Test
    void secondariesSeeTheArtifactThePrimaryProduced() {
        AtomicReference<ReportArtifact> seen = new AtomicReference<>();
        ReportSink primary = (r, o) -> PRIMARY_ARTIFACT;
        ReportSink secondary = (r, o) -> {
            seen.set(r.reportArtifact());
            return null;
        };

        new CompositeReportSink(primary, List.of(secondary)).write(result(), options());

        assertEquals(PRIMARY_ARTIFACT, seen.get());
    }
}
