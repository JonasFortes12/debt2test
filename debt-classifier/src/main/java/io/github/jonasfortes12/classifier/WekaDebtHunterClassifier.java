package io.github.jonasfortes12.classifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.github.jonasfortes12.core.model.ClassificationOptions;
import io.github.jonasfortes12.core.model.ClassifiedDebt;
import io.github.jonasfortes12.core.model.ItemStatus;
import io.github.jonasfortes12.core.model.PipelineError;
import io.github.jonasfortes12.core.model.Provenance;
import io.github.jonasfortes12.core.model.SatdCandidate;
import io.github.jonasfortes12.core.port.DebtClassifier;
import io.github.jonasfortes12.core.result.ClassificationResult;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class WekaDebtHunterClassifier implements DebtClassifier {

    private static final String NONE_DEBT_TYPE = "NONE";
    private static final String SATD_LABEL = "SATD";
    private static final List<String> BINARY_CLASS_VALUES =
            List.of("SATD", "WITHOUT_CLASSIFICATION");
    /** Mirrors the class order in the bundled DebtHunter model metadata. */
    private static final List<String> DEBT_TYPE_VALUES =
            List.of("DESIGN", "TEST", "DOCUMENTATION", "IMPLEMENTATION", "DEFECT");

    private static final Provenance MODEL_PROVENANCE =
            new Provenance("DebtHunter", "model", "configured");
    private static final Provenance FALLBACK_PROVENANCE =
            new Provenance("DebtHunter", "heuristic-fallback", "unavailable");

    private final Classifier binaryClassifier;
    private final Classifier multiClassifier;
    private final String modelInitializationFailure;

    public WekaDebtHunterClassifier(String binaryModelPath, String multiModelPath) {
        Classifier loadedBinary = null;
        Classifier loadedMulti = null;
        String initializationFailure = null;
        try {
            loadedBinary = loadModel(binaryModelPath);
            loadedMulti = loadModel(multiModelPath);
        } catch (Exception ignored) {
            initializationFailure = "DebtHunter models are unavailable";
        }
        if (loadedBinary == null || loadedMulti == null) {
            loadedBinary = null;
            loadedMulti = null;
            initializationFailure = "DebtHunter models are unavailable";
        }
        this.binaryClassifier = loadedBinary;
        this.multiClassifier = loadedMulti;
        this.modelInitializationFailure = initializationFailure;
    }

    private static Classifier loadModel(String modelPath) throws Exception {
        try (InputStream input = Files.newInputStream(Path.of(modelPath))) {
            Object model = weka.core.SerializationHelper.read(input);
            if (!(model instanceof Classifier classifier)) {
                throw new IllegalArgumentException("Serialized model is not a Weka classifier");
            }
            return classifier;
        }
    }

    private static Instances createDataTemplate(String classAttributeName, List<String> classValues) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("comment", (ArrayList<String>) null));
        attributes.add(new Attribute(classAttributeName, new ArrayList<>(classValues)));

        Instances template = new Instances("DebtHunterDataset", attributes, 0);
        template.setClassIndex(template.numAttributes() - 1);
        return template;
    }

    private static Instance createInstance(SatdCandidate candidate, String classAttributeName,
            List<String> classValues) {
        Instances template = createDataTemplate(classAttributeName, classValues);
        Instance instance = new DenseInstance(template.numAttributes());
        instance.setDataset(template);
        Attribute comment = template.attribute("comment");
        instance.setValue(comment.index(), comment.addStringValue(candidate.comment()));
        return instance;
    }

    @Override
    public ClassificationResult classify(List<SatdCandidate> candidates, ClassificationOptions options) {
        Objects.requireNonNull(candidates, "candidates must not be null");
        Objects.requireNonNull(options, "options must not be null");

        List<ClassifiedDebt> classifications = new ArrayList<>();
        List<PipelineError> errors = new ArrayList<>();

        if (!modelsAvailable()) {
            PipelineError modelError = modelFailure(options.allowHeuristicFallback());
            if (!options.allowHeuristicFallback()) {
                return new ClassificationResult(List.of(), List.of(modelError));
            }
            errors.add(modelError);
        }

        for (SatdCandidate candidate : candidates) {
            Objects.requireNonNull(candidate, "candidates must not contain null");
            if (!modelsAvailable()) {
                classifications.add(classifyHeuristically(candidate, List.of()));
                continue;
            }

            try {
                classifications.add(classifyWithModels(candidate));
            } catch (Exception exception) {
                PipelineError predictionError = predictionFailure(candidate, options.allowHeuristicFallback());
                if (options.allowHeuristicFallback()) {
                    classifications.add(classifyHeuristically(candidate, List.of(predictionError)));
                } else {
                    errors.add(predictionError);
                }
            }
        }

        return new ClassificationResult(classifications, errors);
    }

    private boolean modelsAvailable() {
        return binaryClassifier != null && multiClassifier != null;
    }

    private ClassifiedDebt classifyWithModels(SatdCandidate candidate) throws Exception {
        Instance binaryInstance = createInstance(candidate, "BinaryClassification", BINARY_CLASS_VALUES);

        double predBinary = binaryClassifier.classifyInstance(binaryInstance);
        int binaryIndex = predictionIndex(predBinary, BINARY_CLASS_VALUES.size());
        boolean satd = BINARY_CLASS_VALUES.get(binaryIndex).equals(SATD_LABEL);
        Double confidence = confidenceFor(binaryClassifier, binaryInstance, binaryIndex);
        String debtType = NONE_DEBT_TYPE;
        if (satd) {
            Instance multiInstance = createInstance(candidate, "classification", DEBT_TYPE_VALUES);
            double predMulti = multiClassifier.classifyInstance(multiInstance);
            int multiIndex = predictionIndex(predMulti, DEBT_TYPE_VALUES.size());
            debtType = DEBT_TYPE_VALUES.get(multiIndex);
            confidence = confidenceFor(multiClassifier, multiInstance, multiIndex);
        }

        return new ClassifiedDebt(
                candidate,
                satd,
                debtType,
                confidence,
                MODEL_PROVENANCE,
                satd ? ItemStatus.CLASSIFIED : ItemStatus.NOT_SATD,
                List.of());
    }

    private static int predictionIndex(double prediction, int classCount) {
        if (!Double.isFinite(prediction) || prediction != Math.rint(prediction)
                || prediction < 0 || prediction >= classCount) {
            throw new IllegalArgumentException("Weka classifier returned an invalid class index");
        }
        return (int) prediction;
    }

    private static Double confidenceFor(Classifier classifier, Instance instance, int selectedIndex) {
        try {
            double[] distribution = classifier.distributionForInstance(instance);
            if (distribution == null || selectedIndex >= distribution.length) {
                return null;
            }
            double confidence = distribution[selectedIndex];
            return Double.isFinite(confidence) && confidence >= 0 && confidence <= 1
                    ? confidence
                    : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private ClassifiedDebt classifyHeuristically(SatdCandidate candidate, List<PipelineError> errors) {
        String upper = candidate.comment().toUpperCase(Locale.ROOT);
        boolean satd = upper.contains("TODO")
                || upper.contains("FIXME")
                || upper.contains("HACK")
                || upper.contains("TEST");
        String debtType = NONE_DEBT_TYPE;
        if (upper.contains("TEST")) {
            debtType = "TEST";
        } else if (upper.contains("FIXME")) {
            debtType = "DEFECT";
        } else if (upper.contains("TODO") || upper.contains("HACK")) {
            debtType = "DESIGN";
        }

        return new ClassifiedDebt(
                candidate,
                satd,
                debtType,
                null,
                FALLBACK_PROVENANCE,
                satd ? ItemStatus.CLASSIFIED : ItemStatus.NOT_SATD,
                errors);
    }

    private PipelineError modelFailure(boolean fallbackAllowed) {
        String message = modelInitializationFailure == null
                ? "DebtHunter models are unavailable"
                : modelInitializationFailure;
        if (fallbackAllowed) {
            message += "; heuristic fallback was used";
        } else {
            message += "; heuristic fallback is disabled";
        }
        return new PipelineError(
                "classification",
                fallbackAllowed ? "CLASSIFIER_MODEL_FALLBACK" : "CLASSIFIER_MODEL_UNAVAILABLE",
                message,
                null,
                fallbackAllowed);
    }

    private PipelineError predictionFailure(SatdCandidate candidate, boolean fallbackAllowed) {
        return new PipelineError(
                "classification",
                "CLASSIFIER_PREDICTION_FAILED",
                fallbackAllowed
                        ? "DebtHunter prediction failed; heuristic fallback was used"
                        : "DebtHunter prediction failed and heuristic fallback is disabled",
                candidate.candidateId(),
                fallbackAllowed);
    }
}
