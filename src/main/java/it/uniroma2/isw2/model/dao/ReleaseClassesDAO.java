package it.uniroma2.isw2.model.dao;

import it.uniroma2.isw2.model.entity.Release;
import it.uniroma2.isw2.model.entity.ReleaseClasses;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;

/** Accesso a Git per recuperare commit e percorsi Java di una release. */
public class ReleaseClassesDAO {
    private final Path repositoryPath;

    public ReleaseClassesDAO() {
        this(Path.of("../StormDataset/repositories/storm.git"));
    }

    /** Il percorso deve indicare un repository locale con i tag delle release. */
    public ReleaseClassesDAO(Path repositoryPath) {
        this.repositoryPath = Objects.requireNonNull(repositoryPath, "repositoryPath").toAbsolutePath();
    }

    /** Risolve il commit dal tag e legge tutti i percorsi Java del suo albero. */
    public ReleaseClasses getReleaseClasses(Release release) throws IOException {
        Objects.requireNonNull(release, "release");
        Set<String> tags = new LinkedHashSet<>(runGit("tag", "--list").lines().toList());
        ReleaseClasses result = new ReleaseClasses();
        result.setRelease(release);
        result.setCommitHash(resolveCommit(release, tags));
        result.setClassPaths(getClassPaths(result.getCommitHash()));
        return result;
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

