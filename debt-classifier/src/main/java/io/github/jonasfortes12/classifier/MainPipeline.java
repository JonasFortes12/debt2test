package io.github.jonasfortes12.classifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.jonasfortes12.extractor.AstCommentExtractor;
import io.github.jonasfortes12.extractor.AstCommentExtractor.SatdCandidate;
import io.github.jonasfortes12.extractor.GitCloneService;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.stream.Collectors;

public class MainPipeline {

    public static void main(String[] args) {
        String repoUrl = "https://github.com/apache/dubbo";

        // Paths for official pre-trained DebtHunter models
        String binaryModelPath = "preTrainedModels/DHbinaryClassifier.model";
        String multiModelPath = "preTrainedModels/DHmultiClassifier.model";

        System.out.println("=== STARTING HYBRID PIPELINE (MODERN AST + WEKA/DEBTHUNTER) ===");

        // 1. Input: Automated Cloning
        File localRepoDir = GitCloneService.cloneRepository(repoUrl);

        // 2. Processing: Extraction with Modern JavaParser (AST)
        System.out.println("[INFO] Extracting comments and method scope via JavaParser...");
        AstCommentExtractor extractor = new AstCommentExtractor();
        List<SatdCandidate> rawCandidates = extractor.extractFromProject(localRepoDir);
        System.out.println("[INFO] Total comments mapped in methods: " + rawCandidates.size());

        // 3. Classification: Application of DebtHunter Weka Models
        WekaDebtHunterClassifier classifier = new WekaDebtHunterClassifier(binaryModelPath, multiModelPath);
        List<WekaDebtHunterClassifier.ClassifiedDebt> results = rawCandidates.stream()
                .map(classifier::classify)
                .filter(WekaDebtHunterClassifier.ClassifiedDebt::isSatd)
                .collect(Collectors.toList());

        System.out.println("[SUCCESS] Total technical debts (SATD) classified: " + results.size());

        // 4. Visual Output: Console
        printFormattedTable(results);

        // 5. Structured Output: Generation of debt-report.json for the next module
        // Tests (LLM)
        exportJsonReport(results);
    }

    private static void printFormattedTable(List<WekaDebtHunterClassifier.ClassifiedDebt> results) {
        System.out.println("\n--- TABELA DE CLASSIFICAÇÃO DE DÉBITOS TÉCNICOS ---");
        System.out.printf("%-25s | %-15s | %-6s | %-12s | %s%n", "ARQUIVO", "MÉTODO", "LINHA", "TIPO", "COMENTÁRIO");
        System.out.println("-".repeat(105));
        for (var debt : results) {
            String shortFile = new File(debt.getFilePath()).getName();
            System.out.printf("%-25s | %-15s | %-6d | %-12s | %s%n",
                    shortFile,
                    debt.getMethodName(),
                    debt.getLineNumber(),
                    debt.getDebtType(),
                    debt.getComment().replace("\n", " "));
        }
        System.out.println("-".repeat(105));
    }

    private static void exportJsonReport(List<WekaDebtHunterClassifier.ClassifiedDebt> results) {
        try {
            File outputDir = new File("output");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File jsonFile = new File(outputDir, "debt-report.json");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(jsonFile)) {
                gson.toJson(results, writer);
            }
            System.out.println("\n[SUCESSO] debt-report.json gerado em: " + jsonFile.getAbsolutePath());
            System.out.println(
                    "[INFO] Este arquivo contém a localização, comentário e código-fonte associado prontos para alimentar a LLM na geração de testes.");
        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao exportar relatório JSON: " + e.getMessage());
        }
    }
}