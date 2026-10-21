package it.uniroma2.isw2.model.entity;

import java.util.List;

/** Le classi presenti nel commit di una specifica release. */
public class ReleaseClasses {
    private Release release;
    private String commitHash;
    /** Percorsi dei file Java relativi alla radice del repository. */
    private List<String> classPaths;

    public Release getRelease() {
        return release;
    }

    public void setRelease(Release release) {
        this.release = release;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public List<String> getClassPaths() {
        return classPaths;
    }

    public void setClassPaths(List<String> classPaths) {
        this.classPaths = classPaths;
    }
}
