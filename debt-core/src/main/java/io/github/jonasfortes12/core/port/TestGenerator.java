package io.github.jonasfortes12.core.port;

import io.github.jonasfortes12.core.model.EnrichedSatdDebt;
import io.github.jonasfortes12.core.model.TestGenerationOptions;
import io.github.jonasfortes12.core.result.TestGenerationResult;

import java.util.List;

public interface TestGenerator {
    TestGenerationResult generate(List<EnrichedSatdDebt> debts, TestGenerationOptions options);
}
