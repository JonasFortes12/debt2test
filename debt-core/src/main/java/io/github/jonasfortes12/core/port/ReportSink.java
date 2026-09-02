package io.github.jonasfortes12.core.port;

import io.github.jonasfortes12.core.model.PipelineResult;
import io.github.jonasfortes12.core.model.ReportArtifact;
import io.github.jonasfortes12.core.model.ReportOptions;

public interface ReportSink {
    ReportArtifact write(PipelineResult result, ReportOptions options);
}
