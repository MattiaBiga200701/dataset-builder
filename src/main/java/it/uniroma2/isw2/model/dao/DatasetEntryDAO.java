package it.uniroma2.isw2.model.dao;

import it.uniroma2.isw2.model.entity.DatasetEntry;
import it.uniroma2.isw2.model.entity.Release;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Recupera le classi dal commit Git e costruisce le entry della release. */
public class DatasetEntryDAO {
    private final Path repositoryPath;

    public DatasetEntryDAO() {
        this(Path.of("../StormDataset/repositories/storm.git"));
    }

    public DatasetEntryDAO(Path repositoryPath) {
        this.repositoryPath = Objects.requireNonNull(repositoryPath, "repositoryPath").toAbsolutePath();
    }

    public void setProjectName(String projectName) {
        DatasetEntry.setProjectName(projectName);
    }

    public List<DatasetEntry> getEntries(Release release, String commitHash) throws IOException {
        Objects.requireNonNull(release, "release");
        Objects.requireNonNull(commitHash, "commitHash");
        List<DatasetEntry> entries = new ArrayList<>();
        for (String classPath : getClassPaths(commitHash)) {
            DatasetEntry entry = new DatasetEntry();
            entry.setClassName(classPath);
            entry.setRelease(release);
            entries.add(entry);
        }
        return entries;
    }

    /** Percorsi relativi al repository, inclusi test e interfacce, senza checkout. */
    private List<String> getClassPaths(String commitHash) throws IOException {
        String tree = runGit("ls-tree", "-r", "-z", "--name-only", commitHash);
        return Arrays.stream(tree.split("\u0000"))
                .filter(path -> path.endsWith(".java"))
                .sorted()
                .toList();
    }

    private String runGit(String... arguments) throws IOException {
        List<String> command = new ArrayList<>(List.of("git", "-C", repositoryPath.toString()));
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try {
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() != 0) {
                throw new IOException("Comando Git fallito: " + output.strip());
            }
            return output;
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Recupero del commit interrotto", exception);
        } finally {
            process.getInputStream().close();
        }
    }
}
