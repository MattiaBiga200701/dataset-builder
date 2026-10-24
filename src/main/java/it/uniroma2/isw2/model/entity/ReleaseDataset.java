package it.uniroma2.isw2.model.entity;

import java.util.List;


public class ReleaseDataset {

    private Release release;

    private String commitHash;

    private List<DatasetEntry> entries;

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

    public List<DatasetEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<DatasetEntry> entries) {
        this.entries = entries;
    }
}
