package io.github.jonasfortes12.core.port;

import io.github.jonasfortes12.core.model.RepositoryRequest;
import io.github.jonasfortes12.core.model.RepositoryWorkspace;

public interface RepositoryWorkspaceProvider {
    RepositoryWorkspace prepare(RepositoryRequest request);

    default void release(RepositoryWorkspace workspace) {
    }
}
