package io.github.jonasfortes12.classifier;

import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.model.SourceProvenance;
import io.github.jonasfortes12.core.result.ClassificationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WekaDebtHunterClassifierTest {

    private static final String MISSING_BINARY_MODEL = "missing-binary.model";
    private static final String MISSING_MULTI_MODEL = "missing-multi.model";

    @TempDir
    Path temporaryDirectory;

    @Test
    void heuristicFallbackClassifiesSatdWithProvenanceAndRecoverableModelError() {
        ClassificationResult result = fallbackClassifier().classify(
                List.of(candidate("candidate-satd", "TODO: simplify this method")),
                fallbackOptions());

        ClassifiedDebt debt = result.classifications().get(0);
        assertTrue(debt.satd());
        assertEquals(ItemStatus.CLASSIFIED, debt.status());
        assertEquals("candidate-satd", debt.candidateId());
        assertEquals("DebtHunter", debt.provenance().provider());
        assertEquals("heuristic-fallback", debt.provenance().strategy());
        assertEquals("unavailable", debt.provenance().version());
        assertTrue(result.errors().stream().anyMatch(error ->
                error.stage().equals("classification")
                        && error.code().equals("CLASSIFIER_MODEL_FALLBACK")
                        && error.recoverable()));
    }

    @Test
    void ordinaryCommentIsNotSatdWithNormalizedNoneDebtType() {
        ClassificationResult result = fallbackClassifier().classify(
                List.of(candidate("candidate-ordinary", "Explains the normal behavior")),
                fallbackOptions());

        ClassifiedDebt debt = result.classifications().get(0);
        assertFalse(debt.satd());
        assertEquals("NONE", debt.debtType());
        assertEquals(ItemStatus.NOT_SATD, debt.status());
        assertEquals("candidate-ordinary", debt.candidateId());
    }

    @Test
    void disabledFallbackReturnsNoClassificationsWhenModelsAreUnavailable() {
        ClassificationResult result = fallbackClassifier().classify(
                List.of(candidate("candidate-disabled", "TODO: simplify this method")),
                new ClassificationOptions(false));

        assertTrue(result.classifications().isEmpty());
        assertTrue(result.errors().stream().anyMatch(error ->
                error.stage().equals("classification")
                        && error.code().equals("CLASSIFIER_MODEL_UNAVAILABLE")
                        && !error.recoverable()));
    }

    @Test
    void rejectsNullClassificationInputs() {
        WekaDebtHunterClassifier classifier = fallbackClassifier();

        assertThrows(NullPointerException.class,
                () -> classifier.classify(null, new ClassificationOptions(true)));
        assertThrows(NullPointerException.class,
                () -> classifier.classify(List.of(), null));
    }

    @Test
    void modelUsesBundledClassOrdersConfidenceAndProvenance() throws Exception {
        WekaDebtHunterClassifier classifier = classifierWithModels(
                new StubClassifier(0.0, new double[]{0.2, 0.8}, false),
                new StubClassifier(3.0, new double[]{0.1, 0.2, 0.15, 0.45, 0.1}, false));

        ClassificationResult result = classifier.classify(
                List.of(candidate("candidate-model", "ordinary comment")),
                new ClassificationOptions(false));

        ClassifiedDebt debt = result.classifications().get(0);
        assertTrue(debt.satd());
        assertEquals("IMPLEMENTATION", debt.debtType());
        assertEquals(0.45, debt.confidence());
        assertEquals(new io.github.jonasfortes12.core.model.Provenance(
                "DebtHunter", "model", "configured"), debt.provenance());
        assertEquals(ItemStatus.CLASSIFIED, debt.status());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void binaryModelIndexOneMeansNotSatdAndUsesBinaryConfidence() throws Exception {
        WekaDebtHunterClassifier classifier = classifierWithModels(
                new StubClassifier(1.0, new double[]{0.25, 0.75}, false),
                new StubClassifier(3.0, new double[]{0.1, 0.2, 0.15, 0.45, 0.1}, false));

        ClassifiedDebt debt = classifier.classify(
                List.of(candidate("candidate-not-satd", "ordinary comment")),
                new ClassificationOptions(false)).classifications().get(0);

        assertFalse(debt.satd());
        assertEquals("NONE", debt.debtType());
        assertEquals(0.75, debt.confidence());
        assertEquals(ItemStatus.NOT_SATD, debt.status());
    }

    @Test
    void unavailableModelDistributionLeavesConfidenceNullWithoutFailingPrediction() throws Exception {
        WekaDebtHunterClassifier classifier = classifierWithModels(
                new StubClassifier(1.0, null, false),
                new StubClassifier(3.0, null, false));

        ClassifiedDebt debt = classifier.classify(
                List.of(candidate("candidate-no-confidence", "ordinary comment")),
                new ClassificationOptions(false)).classifications().get(0);

        assertFalse(debt.satd());
        assertNull(debt.confidence());
        assertTrue(debt.errors().isEmpty());
    }

    @Test
    void predictionFailureUsesHeuristicFallbackAsAnItemErrorWhenAllowed() throws Exception {
        WekaDebtHunterClassifier classifier = classifierWithModels(
                new StubClassifier(0.0, new double[]{1.0, 0.0}, true),
                new StubClassifier(3.0, new double[]{0.1, 0.2, 0.15, 0.45, 0.1}, false));

        ClassificationResult result = classifier.classify(
                List.of(candidate("candidate-prediction-failure", "TODO: simplify")),
                new ClassificationOptions(true));

        ClassifiedDebt debt = result.classifications().get(0);
        assertTrue(debt.satd());
        assertEquals("DESIGN", debt.debtType());
        assertEquals("heuristic-fallback", debt.provenance().strategy());
        assertTrue(debt.errors().stream().anyMatch(error ->
                error.code().equals("CLASSIFIER_PREDICTION_FAILED")
                        && error.candidateId().equals("candidate-prediction-failure")));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void predictionFailureReturnsNoItemWhenFallbackIsDisabled() throws Exception {
        WekaDebtHunterClassifier classifier = classifierWithModels(
                new StubClassifier(0.0, new double[]{1.0, 0.0}, true),
                new StubClassifier(3.0, new double[]{0.1, 0.2, 0.15, 0.45, 0.1}, false));

        ClassificationResult result = classifier.classify(
                List.of(candidate("candidate-prediction-failure", "TODO: simplify")),
                new ClassificationOptions(false));

        assertTrue(result.classifications().isEmpty());
        assertTrue(result.errors().stream().anyMatch(error ->
                error.code().equals("CLASSIFIER_PREDICTION_FAILED")
                        && error.candidateId().equals("candidate-prediction-failure")
                        && !error.recoverable()));
    }

    @Test
    void repeatedClassificationsDoNotGrowTheCommentDictionary() throws Exception {
        ObservingClassifier.reset();
        WekaDebtHunterClassifier classifier = classifierWithModels(
                new ObservingClassifier(1.0, new double[]{0.2, 0.8}),
                new StubClassifier(3.0, new double[]{0.1, 0.2, 0.15, 0.45, 0.1}, false));
        List<SatdCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            candidates.add(candidate("candidate-" + index, "ordinary comment " + index));
        }

        ClassificationResult result = classifier.classify(candidates, new ClassificationOptions(false));

        assertEquals(100, result.classifications().size());
        assertEquals(1, ObservingClassifier.maximumObservedCommentValues());
    }

    private static WekaDebtHunterClassifier fallbackClassifier() {
        return new WekaDebtHunterClassifier(MISSING_BINARY_MODEL, MISSING_MULTI_MODEL);
    }

    private static ClassificationOptions fallbackOptions() {
        return new ClassificationOptions(true);
    }

    private WekaDebtHunterClassifier classifierWithModels(
            Classifier binaryClassifier, Classifier multiClassifier) throws Exception {
        Path binaryPath = temporaryDirectory.resolve("binary.model");
        Path multiPath = temporaryDirectory.resolve("multi.model");
        writeModel(binaryPath, binaryClassifier);
        writeModel(multiPath, multiClassifier);
        return new WekaDebtHunterClassifier(binaryPath.toString(), multiPath.toString());
    }

    private static void writeModel(Path path, Classifier classifier) throws Exception {
        try (OutputStream output = Files.newOutputStream(path)) {
            SerializationHelper.write(output, classifier);
        }
    }

    private static SatdCandidate candidate(String candidateId, String comment) {
        return new SatdCandidate(
                candidateId,
                "src/Example.java",
                "example",
                10,
                comment,
                "void example() {}",
                new SourceProvenance("https://example.test/repository", "main", "src/Example.java"));
    }

    private static class StubClassifier extends AbstractClassifier {
        private final double prediction;
        private final double[] distribution;
        private final boolean throwOnPrediction;

        private StubClassifier(double prediction, double[] distribution, boolean throwOnPrediction) {
            this.prediction = prediction;
            this.distribution = distribution == null ? null : distribution.clone();
            this.throwOnPrediction = throwOnPrediction;
        }

        @Override
        public void buildClassifier(Instances data) {
        }

        @Override
        public double classifyInstance(Instance instance) throws Exception {
            if (throwOnPrediction) {
                throw new Exception("prediction failure");
            }
            return prediction;
        }

        @Override
        public double[] distributionForInstance(Instance instance) throws Exception {
            if (distribution == null) {
                throw new Exception("distribution unavailable");
            }
            return distribution.clone();
        }
    }

    private static final class ObservingClassifier extends StubClassifier {
        private static final AtomicInteger MAXIMUM_COMMENT_VALUES = new AtomicInteger();

        private ObservingClassifier(double prediction, double[] distribution) {
            super(prediction, distribution, false);
        }

        @Override
        public double classifyInstance(Instance instance) throws Exception {
            MAXIMUM_COMMENT_VALUES.accumulateAndGet(
                    instance.dataset().attribute("comment").numValues(), Math::max);
            return super.classifyInstance(instance);
        }

        private static void reset() {
            MAXIMUM_COMMENT_VALUES.set(0);
        }

        private static int maximumObservedCommentValues() {
            return MAXIMUM_COMMENT_VALUES.get();
        }
    }
}
