package io.github.jonasfortes12.core.port;

import io.github.jonasfortes12.core.model.ExtractionOptions;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;
import io.github.jonasfortes12.core.result.ExtractionResult;

public interface SatdExtractor {
    ExtractionResult extract(RepositoryWorkspace workspace, ExtractionOptions options);
}
