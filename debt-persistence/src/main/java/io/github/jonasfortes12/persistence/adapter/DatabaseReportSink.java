package io.github.jonasfortes12.persistence.adapter;

import java.nio.file.Path;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.ReportArtifact;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.port.ReportSink;
import io.github.jonasfortes12.persistence.repository.PipelineRunRepository;

/**
 * Records where a run's report artifacts were written.
 *
 * <p>Only the artifact paths: everything else was already stored by
 * {@link DatabasePipelineRunStore} during the run. {@code ReportSink.write} receives only a
 * {@link PipelineResult}, whose identifier is the domain run ID (a configuration fingerprint,
 * not a row identity — repeat runs of the same configuration share it), so lookups take the
 * most recently started matching row, and a run whose ID never resolved simply has no paths
 * recorded.
 *
 * <p>Intended as a secondary behind {@code CompositeReportSink}, never as the primary: it
 * returns no file paths of its own, and pipeline artifact validation requires them.
 */
@Component
public class DatabaseReportSink implements ReportSink {

    private final PipelineRunRepository runs;

    public DatabaseReportSink(PipelineRunRepository runs) {
        this.runs = runs;
    }

    @Override
    @Transactional
    public ReportArtifact write(PipelineResult result, ReportOptions options) {
        ReportArtifact artifact = result.reportArtifact();
        if (artifact == null) {
            return null;
        }
        runs.findFirstByRunIdOrderByStartedAtDesc(result.runId()).ifPresent(run -> run.setReportPaths(
                artifact.paths().stream()
                        .map(Path::toString)
                        .collect(Collectors.joining("\n"))));
        return artifact;
    }
}
