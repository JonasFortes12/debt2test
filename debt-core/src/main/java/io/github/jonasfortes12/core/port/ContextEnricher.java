package io.github.jonasfortes12.core.port;

import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ContextRequest;
import io.github.jonasfortes12.core.result.ContextEnrichmentResult;

import java.util.List;

public interface ContextEnricher {
    ContextEnrichmentResult enrich(List<ClassifiedDebt> debts, ContextRequest request);
}
