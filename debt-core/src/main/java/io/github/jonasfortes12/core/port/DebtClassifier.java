package io.github.jonasfortes12.core.port;

import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.result.ClassificationResult;

import java.util.List;

public interface DebtClassifier {
    ClassificationResult classify(List<SatdCandidate> candidates, ClassificationOptions options);
}
