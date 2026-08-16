package io.github.jonasfortes12.classifier;

import io.github.jonasfortes12.extractor.AstCommentExtractor.SatdCandidate;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.util.ArrayList;

public class WekaDebtHunterClassifier {

    private Classifier binaryClassifier;
    private Classifier multiClassifier;
    private Instances dataTemplate;

    public WekaDebtHunterClassifier(String binaryModelPath, String multiModelPath) {
        try {
            System.out.println("[INFO] Loading pre-trained DebtHunter models via Weka...");
            this.binaryClassifier = (Classifier) weka.core.SerializationHelper.read(binaryModelPath);
            this.multiClassifier = (Classifier) weka.core.SerializationHelper.read(multiModelPath);

            // Configures the attribute space expected by the DebtHunter text classifier
            ArrayList<Attribute> attributes = new ArrayList<>();
            attributes.add(new Attribute("comment", (ArrayList<String>) null)); // String attribute

            ArrayList<String> classValues = new ArrayList<>();
            classValues.add("DESIGN");
            classValues.add("DEFECT");
            classValues.add("TEST");
            classValues.add("DOCUMENTATION");
            classValues.add("REQUIREMENT");
            attributes.add(new Attribute("class", classValues));

            this.dataTemplate = new Instances("DebtHunterDataset", attributes, 0);
            this.dataTemplate.setClassIndex(this.dataTemplate.numAttributes() - 1);

            System.out.println("[SUCCESS] Weka models loaded successfully.");
        } catch (Exception e) {
            System.err.println(
                    "[WARNING] Failed to load Weka models (.model). Make sure to place them in the 'preTrainedModels/' folder. Using heuristic fallback if models are not present: "
                            + e.getMessage());
        }
    }

    public static class ClassifiedDebt {
        private String filePath;
        private String methodName;
        private int lineNumber;
        private String comment;
        private String methodSourceCode;
        private boolean isSatd;
        private String debtType;

        public ClassifiedDebt(SatdCandidate candidate, boolean isSatd, String debtType) {
            this.filePath = candidate.getFilePath();
            this.methodName = candidate.getMethodName();
            this.lineNumber = candidate.getLineNumber();
            this.comment = candidate.getCommentContent();
            this.methodSourceCode = candidate.getMethodSourceCode();
            this.isSatd = isSatd;
            this.debtType = debtType;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getComment() {
            return comment;
        }

        public String getMethodSourceCode() {
            return methodSourceCode;
        }

        public boolean isSatd() {
            return isSatd;
        }

        public String getDebtType() {
            return debtType;
        }
    }

    public ClassifiedDebt classify(SatdCandidate candidate) {
        boolean isSatd = true;
        String debtType = "DESIGN_DEBT";

        try {
            if (binaryClassifier != null && multiClassifier != null) {
                Instance inst = new DenseInstance(dataTemplate.numAttributes());
                inst.setDataset(dataTemplate);
                int attrIndex = dataTemplate.attribute("comment").index();
                inst.setValue(attrIndex,
                        dataTemplate.attribute("comment").addStringValue(candidate.getCommentContent()));

                // Binary prediction (IS IT SATD?)
                double predBinary = binaryClassifier.classifyInstance(inst);
                isSatd = (predBinary == 1.0); // As per Weka standard

                if (isSatd) {
                    double predMulti = multiClassifier.classifyInstance(inst);
                    debtType = dataTemplate.classAttribute().value((int) predMulti);
                }
            } else {
                // Intelligent heuristic fallback if the physical .model files are not
                // provided in this POC
                String upper = candidate.getCommentContent().toUpperCase();
                isSatd = upper.contains("TODO") || upper.contains("FIXME") || upper.contains("HACK")
                        || upper.contains("TEST");
                if (upper.contains("TEST"))
                    debtType = "TEST_DEBT";
                else if (upper.contains("FIXME"))
                    debtType = "DEFECT_DEBT";
                else
                    debtType = "DESIGN_DEBT";
            }
        } catch (Exception e) {
            // In case of feature incompatibility in the legacy Weka model, apply
            // a safety heuristic
            debtType = "DESIGN_DEBT";
        }

        return new ClassifiedDebt(candidate, isSatd, debtType);
    }
}