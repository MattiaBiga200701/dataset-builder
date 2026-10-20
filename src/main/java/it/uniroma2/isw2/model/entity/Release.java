package it.uniroma2.isw2.model.entity;

import java.time.LocalDateTime;

/** Una release con i quattro dati esportati dallo script GetVersionsFromJIRA. */
public class Release {
    /** Index: progressivo assegnato dopo l'ordinamento cronologico, a partire da 1. */
    private Integer index;

    /** Version ID: identificativo della versione su Jira. */
    private String versionId;

    /** Version Name: nome della versione su Jira. */
    private String versionName;

    /** Date: data di rilascio convertita a inizio giornata, come nello script. */
    private LocalDateTime date;

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
