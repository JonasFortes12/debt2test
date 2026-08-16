package io.github.jonasfortes12.extractor;

import org.eclipse.jgit.api.Git;
import java.io.File;
import java.nio.file.Files;

public class GitCloneService {

    public static File cloneRepository(String repoUrl) {
        try {
            File localPath = Files.createTempDirectory("debt2test-repo-").toFile();
            if (localPath.exists()) {
                localPath.delete();
            }

            System.out.println("[INFO] Cloning repository from: " + repoUrl + " ...");
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(localPath)
                    .call();

            System.out.println("[SUCCESS] Repository cloned at: " + localPath.getAbsolutePath());
            return localPath;
        } catch (Exception e) {
            throw new RuntimeException("[FAILURE] Error while cloning git repository: " + e.getMessage(), e);
        }
    }
}