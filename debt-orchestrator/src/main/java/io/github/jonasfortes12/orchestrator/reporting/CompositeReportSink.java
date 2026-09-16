package io.github.jonasfortes12.orchestrator.reporting;

import java.util.List;
import java.util.Objects;

import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.ReportArtifact;
import io.github.jonasfortes12.core.model.ReportOptions;
import io.github.jonasfortes12.core.port.ReportSink;
import io.github.jonasfortes12.orchestrator.util.OrchestrationUtils;

/**
 * Writes a report through a primary sink and then through any secondaries.
 *
 * <p>The primary owns the returned artifact, because pipeline artifact validation requires real
 * file paths. Secondaries receive the result with that artifact already attached, since the
 * application service only attaches it after this call returns.
 *
 * <p>A secondary that fails is logged and skipped: losing the database copy of a report must not
 * cost the run its file report. A failing primary still propagates, because a missing artifact is
 * a genuine run failure.
 */
public final class CompositeReportSink implements ReportSink {

    private final ReportSink primary;
    private final List<ReportSink> secondaries;

    public CompositeReportSink(ReportSink primary, List<ReportSink> secondaries) {
        this.primary = Objects.requireNonNull(primary, "primary must not be null");
        this.secondaries = List.copyOf(Objects.requireNonNull(secondaries, "secondaries must not be null"));
    }

    @Override
    public ReportArtifact write(PipelineResult result, ReportOptions options) {
        ReportArtifact artifact = primary.write(result, options);
        PipelineResult withArtifact = artifact == null ? result : result.withReportArtifact(artifact);
        for (ReportSink secondary : secondaries) {
            try {
                secondary.write(withArtifact, options);
            } catch (Exception ignored) {
                OrchestrationUtils.logPipeline("secondary report sink failed; continuing");
            }
        }
        return artifact;
    }
}
