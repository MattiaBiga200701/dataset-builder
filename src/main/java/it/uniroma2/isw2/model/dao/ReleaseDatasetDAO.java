package it.uniroma2.isw2.model.dao;

import it.uniroma2.isw2.model.entity.DatasetEntry;
import it.uniroma2.isw2.model.entity.Release;
import it.uniroma2.isw2.model.entity.ReleaseDataset;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;

/** Accesso a Git per recuperare commit e percorsi Java di una release. */
public class ReleaseDatasetDAO {
    private final Path repositoryPath;

    public ReleaseDatasetDAO() {
        this(Path.of("../StormDataset/repositories/storm.git"));
    }

    /** Il percorso deve indicare un repository locale con i tag delle release. */
    public ReleaseDatasetDAO(Path repositoryPath) {
        this.repositoryPath = Objects.requireNonNull(repositoryPath, "repositoryPath").toAbsolutePath();
    }

    /** Risolve il commit dal tag e crea le entry delle classi Java, con metriche da calcolare. */
    public ReleaseDataset getReleaseDataset(Release release) throws IOException {
        Objects.requireNonNull(release, "release");
        prepareRepository();
        Set<String> tags = new LinkedHashSet<>(runGit("tag", "--list").lines().toList());
        ReleaseDataset result = new ReleaseDataset();
        result.setRelease(release);
        result.setCommitHash(resolveCommit(release, tags));
        List<DatasetEntry> entries = new ArrayList<>();
        for (String classPath : getClassPaths(result.getCommitHash())) {
            DatasetEntry entry = new DatasetEntry();
            entry.setClassName(classPath);
            entry.setRelease(release);
            entries.add(entry);
        }
        result.setEntries(entries);
        return result;
    }
    private void prepareRepository() throws IOException {
        if (!Files.exists(repositoryPath)) {
            Files.createDirectories(repositoryPath.getParent());
            Process process = new ProcessBuilder("git", "clone", "--bare",
                    "https://github.com/apache/storm.git", repositoryPath.toString())
                    .inheritIO().start();
            try {
                if (process.waitFor() != 0) {
                    throw new IOException("Clone STORM fallito: " + repositoryPath);
                }
            } catch (InterruptedException exception) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new IOException("Download di STORM interrotto", exception);
            }
        }
        if (!runGit("rev-parse", "--is-shallow-repository").strip().equals("false")) {
            throw new IOException("Serve la cronologia Git completa: " + repositoryPath);
        }
    }

    /** Risolve anche i tag annotati al commit, senza usare la data come fallback. */
    private String resolveCommit(Release release, Set<String> tags) throws IOException {
        String version = release.getVersionName();
        if (version == null || version.isBlank()) {
            throw new IOException("Nome versione assente per la release " + release.getVersionId());
        }
        Set<String> names = new LinkedHashSet<>();
        names.add(version);
        if (version.endsWith("-incubating")) {
            names.add(version.substring(0, version.length() - "-incubating".length()));
        }
        Set<String> commits = new LinkedHashSet<>();
        for (String name : names) {
            for (String prefix : List.of("", "v", "storm-", "apache-storm-", "release-")) {
                String tag = prefix + name;
                if (tags.contains(tag)) {
                    commits.add(runGit("rev-parse", "--verify", "--end-of-options",
                            "refs/tags/" + tag + "^{commit}").strip());
                }
            }
        }
        if (commits.size() != 1) {
            throw new IOException("Release " + version + " (Jira " + release.getVersionId()
                    + "): " + (commits.isEmpty() ? "nessun tag trovato" : "tag con commit differenti"));
        }
        return commits.iterator().next();
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

